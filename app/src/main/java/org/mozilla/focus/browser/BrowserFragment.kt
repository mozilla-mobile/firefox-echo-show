/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.coroutines.experimental.CancellationException
import org.mozilla.focus.R
import org.mozilla.focus.UrlSearcher
import org.mozilla.focus.browser.URLs.APP_STARTUP_HOME
import org.mozilla.focus.ext.getAccessibilityManager
import org.mozilla.focus.ext.isVisible
import org.mozilla.focus.ext.isVoiceViewEnabled
import org.mozilla.focus.ext.toUri
import org.mozilla.focus.home.BundledTilesManager
import org.mozilla.focus.home.CustomTilesManager
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.iwebview.IWebViewLifecycleFragment
import org.mozilla.focus.session.NullSession
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionCallbackProxy
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.AppStartupTimeMeasurement
import org.mozilla.focus.telemetry.LoadTimeObserver
import org.mozilla.focus.toolbar.ToolbarEvent
import org.mozilla.focus.toolbar.ToolbarStateProvider
import org.mozilla.focus.utils.ToastManager

private const val ARGUMENT_SESSION_UUID = "sessionUUID"

private val URLS_BLOCKED_FROM_USERS = setOf(
        APP_STARTUP_HOME.toString()
)

/** An interface expected to be implemented by the Activities that create a BrowserFragment. */
interface BrowserFragmentCallbacks : HomeTileLongClickListener {
    fun onHomeVisibilityChange(isHomeVisible: Boolean, isHomescreenOnStartup: Boolean)
    fun onFullScreenChange(isFullscreen: Boolean)

    fun onNonTextInputUrlEntered(urlStr: String)
}

interface HomeTileLongClickListener {
    fun onHomeTileLongClick(unpinTile: () -> Unit)
}

/**
 * Fragment for displaying the browser UI.
 */
class BrowserFragment : IWebViewLifecycleFragment() {
    companion object {
        const val FRAGMENT_TAG = "browser"

        @JvmStatic
        fun createForSession(session: Session) = BrowserFragment().apply {
            arguments = Bundle().apply { putString(ARGUMENT_SESSION_UUID, session.uuid) }
        }
    }

    // IWebViewLifecycleFragment expects a value for these properties before onViewCreated. We use a getter
    // for the properties that reference session because it is lateinit.
    override lateinit var session: Session
    override val initialUrl get() = session.url.value
    override lateinit var iWebViewCallback: IWebView.Callback

    internal val callbacks: BrowserFragmentCallbacks? get() = activity as BrowserFragmentCallbacks?
    val toolbarStateProvider = BrowserToolbarStateProvider()
    var onUrlUpdate: ((url: String?) -> Unit)? = null
    var onSessionLoadingUpdate: ((isLoading: Boolean) -> Unit)? = null
    var onSessionProgressUpdate: ((value: Int) -> Unit)? = null
    private var touchExplorationStateChangeListener: TouchExplorationStateChangeListener? = null

    /**
     * The current URL.
     *
     * Use this instead of the WebView's URL which can return null, return a null URL, or return
     * data: URLs (for error pages).
     */
    var url: String? = null
        private set(value) {
            field = value

            // We prevent users from typing this URL in loadUrl but this will still be called for
            // the initial URL set in the Session.
            if (url == APP_STARTUP_HOME.toString()) {
                homeScreen.visibility = View.VISIBLE
            }

            onUrlUpdate?.invoke(url) // This should be called last so app state is up-to-date.
        }

    // If the URL is startup home, the home screen should always be visible. For defensiveness, we
    // also check this condition. It's probably not necessary (it was originally added when the startup
    // url was the empty string which I was concerned the WebView could pass to us while loading).
    private val isStartupHomepageVisible: Boolean get() = url == APP_STARTUP_HOME.toString() && homeScreen.isVisible

    private val sessionManager = SessionManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = initSession()
        webView?.setBlockingEnabled(session.isBlockingEnabled)
        iWebViewCallback = SessionCallbackProxy(session, FullscreenCallbacks(this))

        LoadTimeObserver.addObservers(session, this)
    }

    override fun onResume() {
        super.onResume()
        AppStartupTimeMeasurement.fragmentOnResume()
    }

    private fun initSession(): Session {
        val sessionUUID = arguments?.getString(ARGUMENT_SESSION_UUID)
                ?: throw IllegalAccessError("No session exists")
        val session = if (sessionManager.hasSessionWithUUID(sessionUUID))
            sessionManager.getSessionByUUID(sessionUUID)
        else
            NullSession()

        session.url.observe(this, Observer { url -> this@BrowserFragment.url = url })
        session.loading.observe(this, SessionLoadingObserver())
        session.progress.observe(this, Observer { it?.let { onSessionProgressUpdate?.invoke(it) } })
        return session
    }

    fun onToolbarEvent(event: ToolbarEvent, value: String?) {
        val context = context!!
        when (event) {
            ToolbarEvent.BACK -> if (webView?.canGoBack() ?: false) webView?.goBack()
            ToolbarEvent.FORWARD -> if (webView?.canGoForward() ?: false) webView?.goForward()
            ToolbarEvent.TURBO -> {
                when (value) {
                    ToolbarEvent.VAL_CHECKED -> {
                        ToastManager.showToast(R.string.turbo_mode_enabled_toast, context)
                    }
                    ToolbarEvent.VAL_UNCHECKED -> {
                        ToastManager.showToast(R.string.turbo_mode_disabled_toast, context)
                    }
                }
                webView?.reload()
            }
            ToolbarEvent.RELOAD -> webView?.reload()
            ToolbarEvent.SETTINGS -> Unit // No Settings in BrowserFragment
            ToolbarEvent.PIN_ACTION -> this@BrowserFragment.url?.let { url -> onPinToolbarEvent(context, url, value) }
            ToolbarEvent.HOME -> if (!homeScreen.isVisible) { homeScreen.setVisibilityWithAnimation(toShow = true)
            }

            ToolbarEvent.LOAD_URL -> throw IllegalStateException("Expected $event to be handled sooner")
        }
        Unit
    }

    private fun onPinToolbarEvent(context: Context, url: String, value: String?) {
        when (value) {
            ToolbarEvent.VAL_CHECKED -> {
                CustomTilesManager.getInstance(context).pinSite(context, url,
                        webView?.takeScreenshot())
                homeScreen.refreshTilesForInsertion()
                ToastManager.showPinnedToast(context)
            }
            ToolbarEvent.VAL_UNCHECKED -> {
                url.toUri()?.let {
                    val tileId = BundledTilesManager.getInstance(context).unpinSite(context, it)
                            ?: CustomTilesManager.getInstance(context).unpinSite(context, url)
                    // tileId should never be null, unless, for some reason we don't
                    // have a reference to the tile/the tile isn't a Bundled or Custom tile
                    if (tileId != null && !tileId.isEmpty()) {
                        homeScreen.removePinnedSiteFromTiles(tileId)
                        ToastManager.showUnpinnedToast(context)
                    }
                }
            }
            else -> throw IllegalArgumentException("Unexpected value for PIN_ACTION: " + value)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        with(layout.homeScreen) {
            onTileClicked = { callbacks?.onNonTextInputUrlEntered(it) }
            urlSearcher = activity as UrlSearcher
            visibility = View.GONE
            onPreSetVisibilityListener = { isHomeVisible ->
                // It's a pre-set-visibility listener so we can't use toolbarStateProvider.isStartupHomePageVisible.
                callbacks?.onHomeVisibilityChange(isHomeVisible, url == APP_STARTUP_HOME.toString())
                updateWebViewVisibility(isVoiceViewEnabled = context.isVoiceViewEnabled(), isHomeVisible = isHomeVisible)
            }
            homeTileLongClickListener = object : HomeTileLongClickListener {
                override fun onHomeTileLongClick(unpinTile: () -> Unit) {
                    callbacks?.onHomeTileLongClick(unpinTile)
                }
            }
        }

        touchExplorationStateChangeListener = BrowserTouchExplorationStateChangeListener(
                layout.homeScreen, this::updateWebViewVisibility).also {
            layout.context.getAccessibilityManager().addTouchExplorationStateChangeListener(it)
        }

        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()

        context?.getAccessibilityManager()?.removeTouchExplorationStateChangeListener(touchExplorationStateChangeListener)
        touchExplorationStateChangeListener = null

        // Since we start the async jobs in View.init and Android is inflating the view for us,
        // there's no good way to pass in the uiLifecycleJob. We could consider other solutions
        // but it'll add complexity that I don't think is probably worth it.
        homeScreen.uiLifecycleCancelJob.cancel(CancellationException("Parent lifecycle has ended"))
    }

    fun loadUrl(url: String) {
        // Intents can trigger loadUrl, and we need to make sure the homescreen is always hidden.
        homeScreen.setVisibilityWithAnimation(toShow = false)
        val webView = webView
        if (webView != null && !TextUtils.isEmpty(url) && !URLS_BLOCKED_FROM_USERS.contains(url)) {
            webView.loadUrl(url)
        }
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        /**
         * Key handling order:
         * - Menu to control overlay
         * - Youtube remap of BACK to ESC
         * - Return false, as unhandled
         */
        return handleSpecialKeyEvent(event)
    }

    private fun updateWebViewVisibility(isVoiceViewEnabled: Boolean, isHomeVisible: Boolean) {
        // We want to disable accessibility on the WebView when the home screen is visible so users
        // cannot focus the WebView content below home tiles. Unfortunately, isFocusable* and
        // setImportantForAccessibility didn't work so the only way I could disable WebView
        // accessibility was to hide it. However, hiding it here looks bad for visual users and
        // hiding it in conjunction with home screen animations adds complexity. Also, future designs
        // display the home tiles over the partially visible, unfocusable WebView, invalidating the
        // hide-it-for-everyone approach so it seemed simpler to only hide the WebView for a11y users
        // in this simple place.
        val isWebViewVisible = !isVoiceViewEnabled || !isHomeVisible
        webView?.setVisibility(if (isWebViewVisible) View.VISIBLE else View.GONE)
    }

    private fun handleSpecialKeyEvent(event: KeyEvent): Boolean {
        if (!homeScreen.isVisible && webView!!.isYoutubeTV &&
                event.keyCode == KeyEvent.KEYCODE_BACK) {
            val escKeyEvent = KeyEvent(event.action, KeyEvent.KEYCODE_ESCAPE)
            activity?.dispatchKeyEvent(escKeyEvent)
            return true
        }
        return false
    }

    inner class BrowserToolbarStateProvider : ToolbarStateProvider {
        override fun isBackEnabled() = webView?.canGoBack() ?: false
        override fun isForwardEnabled() = webView?.canGoForward() ?: false
        override fun isStartupHomepageVisible() = isStartupHomepageVisible
        override fun getCurrentUrl() = url
        override fun isURLPinned() = url.toUri()?.let {
            // TODO: #569 fix CustomTilesManager to use Uri too
            CustomTilesManager.getInstance(context!!).isURLPinned(it.toString()) ||
                    BundledTilesManager.getInstance(context!!).isURLPinned(it) } ?: false
    }

    private inner class SessionLoadingObserver : Observer<Boolean> {
        override fun onChanged(isLoading: Boolean?) {
            if (isLoading == null) { return }
            onSessionLoadingUpdate?.invoke(isLoading)

            val uri = url?.toUri() ?: return
            val webView = webView ?: return
            WebCompat.onSessionLoadingChanged(isLoading, uri, webView)
        }
    }
}

private class BrowserTouchExplorationStateChangeListener(
    private val homeScreen: HomeTileGridNavigation,
    private val updateWebViewVisibility: (isVoiceViewEnabled: Boolean, isHomeVisible: Boolean) -> Unit
) : TouchExplorationStateChangeListener {
    override fun onTouchExplorationStateChanged(isVoiceViewEnabled: Boolean) { // touch exploration state = VoiceView
        updateWebViewVisibility(isVoiceViewEnabled, homeScreen.isVisible)
    }
}

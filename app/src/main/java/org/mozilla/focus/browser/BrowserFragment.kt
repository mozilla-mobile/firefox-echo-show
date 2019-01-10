/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import org.mozilla.focus.ActiveScreen.NAVIGATION_OVERLAY_ON_STARTUP
import org.mozilla.focus.R
import org.mozilla.focus.architecture.FirefoxViewModelProviders
import org.mozilla.focus.browser.URLs.APP_STARTUP_HOME
import org.mozilla.focus.ext.getAccessibilityManager
import org.mozilla.focus.ext.getNavigationOverlay
import org.mozilla.focus.ext.isVisibleAndNonNull
import org.mozilla.focus.ext.isVoiceViewEnabled
import org.mozilla.focus.ext.toUri
import org.mozilla.focus.home.BundledTilesManager
import org.mozilla.focus.home.CustomTilesManager
import org.mozilla.focus.home.NavigationOverlayFragment
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
    fun onFullScreenChange(isFullscreen: Boolean)

    fun onNonTextInputUrlEntered(urlStr: String)

    fun onUrlUpdate(url: String?)
    fun onSessionLoadingUpdate(isLoading: Boolean)
    fun onSessionProgressUpdate(progress: Int)
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

    private var viewModel: BrowserViewModel? = null
    internal val callbacks: BrowserFragmentCallbacks? get() = activity as BrowserFragmentCallbacks?
    val toolbarStateProvider = BrowserToolbarStateProvider()
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
                viewModel?.startupUrlSet()
            }

            callbacks?.onUrlUpdate(url) // This should be called last so app state is up-to-date.
        }

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
        session.progress.observe(this, Observer { it?.let { callbacks?.onSessionProgressUpdate(it) } })
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

            ToolbarEvent.LOAD_URL -> throw IllegalStateException("Expected $event to be handled sooner")
            ToolbarEvent.HOME -> throw java.lang.IllegalStateException("Expected $event to be handled by ToolbarViewModel")
        }
        Unit
    }

    private fun onPinToolbarEvent(context: Context, url: String, value: String?) {
        when (value) {
            ToolbarEvent.VAL_CHECKED -> {
                CustomTilesManager.getInstance(context).pinSite(context, url,
                        webView?.takeScreenshot())
                fragmentManager.getNavigationOverlay()?.refreshTilesForInsertion()
                ToastManager.showPinnedToast(context)
            }
            ToolbarEvent.VAL_UNCHECKED -> {
                url.toUri()?.let {
                    val tileId = BundledTilesManager.getInstance(context).unpinSite(context, it)
                            ?: CustomTilesManager.getInstance(context).unpinSite(context, url)
                    // tileId should never be null, unless, for some reason we don't
                    // have a reference to the tile/the tile isn't a Bundled or Custom tile
                    if (tileId != null && !tileId.isEmpty()) {
                        fragmentManager.getNavigationOverlay()?.removePinnedSiteFromTiles(tileId)
                        ToastManager.showUnpinnedToast(context)
                    }
                }
            }
            else -> throw IllegalArgumentException("Unexpected value for PIN_ACTION: " + value)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val viewModel = FirefoxViewModelProviders.of(this)[BrowserViewModel::class.java]
        this.viewModel = viewModel

        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        // todo: test that bug is fixed.
        // todo: viewLifecycleOwner, newer version? SDK 28 and androidX 1.0.0
        viewModel.activeScreen.observe(this, Observer {
            updateWebViewVisibility(isVoiceViewEnabled = context!!.isVoiceViewEnabled(),
                isHomescreenOnStartup = it!! == NAVIGATION_OVERLAY_ON_STARTUP)
        })

        touchExplorationStateChangeListener = BrowserTouchExplorationStateChangeListener(
                fragmentManager::getNavigationOverlay, this::updateWebViewVisibility).also {
            layout.context.getAccessibilityManager().addTouchExplorationStateChangeListener(it)
        }

        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()

        context?.getAccessibilityManager()?.removeTouchExplorationStateChangeListener(touchExplorationStateChangeListener)
        touchExplorationStateChangeListener = null
    }

    fun loadUrl(url: String) {
        // Intents can trigger loadUrl, and we need to make sure the navigation overlay is always hidden.
        viewModel?.loadUrlCalled()
        val webView = webView
        if (webView != null && !TextUtils.isEmpty(url) && !URLS_BLOCKED_FROM_USERS.contains(url)) {
            webView.loadUrl(url)
        }
    }

    private fun updateWebViewVisibility(
        isVoiceViewEnabled: Boolean,
        isHomescreenOnStartup: Boolean
    ) {
        // We want to disable accessibility on the WebView when the home screen is visible so users
        // cannot focus the WebView content below home tiles. Unfortunately, isFocusable* and
        // setImportantForAccessibility didn't work so the only way I could disable WebView
        // accessibility was to hide it. However, hiding it here looks bad for visual users and
        // hiding it in conjunction with home screen animations adds complexity. Also, future designs
        // display the home tiles over the partially visible, unfocusable WebView, invalidating the
        // hide-it-for-everyone approach so it seemed simpler to only hide the WebView for a11y users
        // in this simple place.
        val isWebViewHidden = isVoiceViewEnabled && isHomescreenOnStartup
        webView?.setVisibility(if (isWebViewHidden) View.GONE else View.VISIBLE)
    }

    inner class BrowserToolbarStateProvider : ToolbarStateProvider {
        override fun isBackEnabled() = webView?.canGoBack() ?: false
        override fun isForwardEnabled() = webView?.canGoForward() ?: false
        override fun isStartupHomepageVisible() = viewModel?.activeScreen == NAVIGATION_OVERLAY_ON_STARTUP
        override fun getCurrentUrl() = url
        override fun isURLPinned() = url.toUri()?.let {
            // TODO: #569 fix CustomTilesManager to use Uri too
            CustomTilesManager.getInstance(context!!).isURLPinned(it.toString()) ||
                    BundledTilesManager.getInstance(context!!).isURLPinned(it) } ?: false
    }

    private inner class SessionLoadingObserver : Observer<Boolean> {
        override fun onChanged(isLoading: Boolean?) {
            if (isLoading == null) { return }
            callbacks?.onSessionLoadingUpdate(isLoading)

            val uri = url?.toUri() ?: return
            val webView = webView ?: return
            WebCompat.onSessionLoadingChanged(isLoading, uri, webView)
        }
    }
}

private class BrowserTouchExplorationStateChangeListener(
    private val navigationOverlayProvider: () -> NavigationOverlayFragment?,
    private val updateWebViewVisibility: (isVoiceViewEnabled: Boolean, isHomeVisible: Boolean) -> Unit
) : TouchExplorationStateChangeListener {
    override fun onTouchExplorationStateChanged(isVoiceViewEnabled: Boolean) { // touch exploration state = VoiceView
        updateWebViewVisibility(isVoiceViewEnabled, navigationOverlayProvider().isVisibleAndNonNull)
    }
}

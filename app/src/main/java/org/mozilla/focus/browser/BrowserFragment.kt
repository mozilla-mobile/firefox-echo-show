/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.annotation.UiThread
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_browser.*
import org.mozilla.focus.R
import org.mozilla.focus.architecture.FirefoxViewModelProviders
import org.mozilla.focus.browser.URLs.APP_STARTUP_HOME
import org.mozilla.focus.ext.getNavigationOverlay
import org.mozilla.focus.ext.isVisibleAndNonNull
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

    @UiThread // performs a fragment transaction.
    fun setNavigationOverlayIsVisible(isVisible: Boolean, isOverlayOnStartup: Boolean = false)

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

    internal val callbacks: BrowserFragmentCallbacks? get() = activity as BrowserFragmentCallbacks?
    val toolbarStateProvider = BrowserToolbarStateProvider()
    private var isWebViewVisibleObserver: IsWebViewVisibleViewModelObserver? = null
    private var isFullscreenBackgroundEnabledObserver: IsFullscreenBackgroundEnabledObserver? = null

    private val viewModel: BrowserViewModel
        get() = FirefoxViewModelProviders.of(this)[BrowserViewModel::class.java]

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
                callbacks?.setNavigationOverlayIsVisible(true, isOverlayOnStartup = true)
            }

            callbacks?.onUrlUpdate(url) // This should be called last so app state is up-to-date.
        }

    // If the URL is startup home, the home screen should always be visible. For defensiveness, we
    // also check this condition. It's probably not necessary (it was originally added when the startup
    // url was the empty string which I was concerned the WebView could pass to us while loading).
    private val isStartupHomepageVisible: Boolean
        get() = url == APP_STARTUP_HOME.toString() && fragmentManager.getNavigationOverlay().isVisibleAndNonNull

    private val sessionManager = SessionManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = initSession()
        webView?.setBlockingEnabled(session.isBlockingEnabled)
        iWebViewCallback = SessionCallbackProxy(session, FullscreenCallbacks(this, viewModel))

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
            ToolbarEvent.HOME -> if (!fragmentManager.getNavigationOverlay().isVisibleAndNonNull) {
                callbacks?.setNavigationOverlayIsVisible(true)
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
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        if (isWebViewVisibleObserver != null || isFullscreenBackgroundEnabledObserver != null) {
            throw IllegalStateException("view observers unexpectedly exist: bad lifecycle assumptions?")
        }
        isWebViewVisibleObserver = IsWebViewVisibleViewModelObserver().also {
            // TODO: After SDK 28 upgrade (#72), observe over viewLifecycleOwner, remove obs removal in destroyView.
            viewModel.isWebViewVisible.observe(this@BrowserFragment, it)
        }
        isFullscreenBackgroundEnabledObserver = IsFullscreenBackgroundEnabledObserver().also {
            // TODO: update to viewLifecycleObserver in SDK 28 change.
            viewModel.isWindowBackgroundEnabled.observe(this@BrowserFragment, it)
        }

        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()

        isWebViewVisibleObserver?.let {
            viewModel.isWebViewVisible.removeObserver(it)
            isWebViewVisibleObserver = null
        }
        isFullscreenBackgroundEnabledObserver?.let {
            viewModel.isWindowBackgroundEnabled.removeObserver(it)
            isFullscreenBackgroundEnabledObserver = null
        }
    }

    fun onNavigationOverlayVisibilityChange(isVisible: Boolean) {
        viewModel.onNavigationOverlayVisibilityChange(isVisible)
    }

    fun loadUrl(url: String) {
        // Intents can trigger loadUrl, and we need to make sure the navigation overlay is always hidden.
        callbacks?.setNavigationOverlayIsVisible(false)
        val webView = webView
        if (webView != null && !TextUtils.isEmpty(url) && !URLS_BLOCKED_FROM_USERS.contains(url)) {
            webView.loadUrl(url)
        }
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
            callbacks?.onSessionLoadingUpdate(isLoading)

            val uri = url?.toUri() ?: return
            val webView = webView ?: return
            WebCompat.onSessionLoadingChanged(isLoading, uri, webView)
        }
    }

    private inner class IsWebViewVisibleViewModelObserver : Observer<Boolean> {
        override fun onChanged(isVisible: Boolean?) {
            webView?.setVisibility(if (isVisible!!) View.VISIBLE else View.GONE)
        }
    }

    private inner class IsFullscreenBackgroundEnabledObserver : Observer<Boolean> {
        override fun onChanged(isEnabled: Boolean?) {
            fullscreenContainerBackground.visibility = if (isEnabled == true) View.VISIBLE else View.GONE
        }
    }
}

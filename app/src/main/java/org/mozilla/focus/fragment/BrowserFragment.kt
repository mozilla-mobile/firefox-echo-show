/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.ext.isVoiceViewEnabled
import org.mozilla.focus.session.NullSession
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionCallbackProxy
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Direction
import org.mozilla.focus.utils.Edge
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.widget.CursorEvent

private const val ARGUMENT_SESSION_UUID = "sessionUUID"
private const val SCROLL_MULTIPLIER = 45

/**
 * Fragment for displaying the browser UI.
 */
class BrowserFragment : WebFragment(), CursorEvent {
    companion object {
        const val FRAGMENT_TAG = "browser"

        @JvmStatic
        fun createForSession(session: Session) = BrowserFragment().apply {
            arguments = Bundle().apply { putString(ARGUMENT_SESSION_UUID, session.uuid) }
        }
    }

    // getUrl() is used for things like sharing the current URL. We could try to use the webview,
    // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
    // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
    // user is probably expecting to share, so lets use that here:
    //
    // Note: when refactoring, I removed the url view and replaced urlView.setText with assignment
    // to this url variable - should be equivalent.
    var url: String? = null
        private set
    private val sessionManager = SessionManager.getInstance()
    private lateinit var session: Session

    val cursorLocation get() = cursor.location
    private val scrollVelocity get() = cursor.speed.toInt() * SCROLL_MULTIPLIER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()
    }

    private fun initSession() {
        val sessionUUID = arguments.getString(ARGUMENT_SESSION_UUID)
                ?: throw IllegalAccessError("No session exists")
        session = if (sessionManager.hasSessionWithUUID(sessionUUID))
            sessionManager.getSessionByUUID(sessionUUID)
        else
            NullSession()

        setBlockingEnabled(session.isBlockingEnabled)
        session.url.observe(this, Observer { url -> this@BrowserFragment.url = url })
        session.loading.observe(this, object : NonNullObserver<Boolean>() {
            public override fun onValueChanged(loading: Boolean) {
                val activity = activity as MainActivity
                updateCursorState()
                if (!loading && activity.isReloadingForYoutubeDrawerClosed) {
                    activity.isReloadingForYoutubeDrawerClosed = false

                    // We send a play event which:
                    // - If we're on the video selection page, does nothing.
                    // - If we're in a fullscreen video, will show the play/pause controls on the screen so
                    // we don't just see a black screen.
                    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.updateHintNavigationVisibility(MainActivity.VideoPlayerState.BROWSER)
    }

    // TODO: if we convert WebFragment to kotlin, these can become abstract properties
    override fun getSession(): Session {
        return session
    }

    override fun getInitialUrl(): String? {
        return session.url.value
    }

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.fragment_browser, container, false).apply {
                cursor.cursorEvent = this@BrowserFragment
            }

    override fun createCallback() = SessionCallbackProxy(session, BrowserIWebViewCallback(this))

    fun onBackPressed(): Boolean {
        if (canGoBack()) {
            // Go back in web history
            goBack()
            TelemetryWrapper.browserBackControllerEvent()
        } else {
            fragmentManager.popBackStack()
            SessionManager.getInstance().removeCurrentSession()
        }

        return true
    }

    // TODO: When all calling code is kotlin, rm these - they're unnecessary with cascading nulls.
    fun canGoForward() = webview?.canGoForward() ?: false
    fun canGoBack() = webview?.canGoBack() ?: false
    fun goBack() = webview?.goBack()
    fun goForward() = webview?.goForward()

    fun loadUrl(url: String) {
        val webView = webView
        if (webView != null && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url)
        }
    }

    fun reload() = webView?.reload()
    fun setBlockingEnabled(enabled: Boolean) = webview?.setBlockingEnabled(enabled)

    // --- TODO: CURSOR CODE - MODULARIZE IN #412. --- //
    /**
     * Gets the current state of the application and updates the cursor state accordingly.
     *
     * Note that this pattern could use some improvements:
     * - It's a little weird to get the current state from globals, rather than get passed in relevant values.
     * - BrowserFragment.setCursorEnabled should be called from this code path, but that's unclear
     * - BrowserFragment should use a listener to talk to MainActivity and shouldn't know about it directly.
     * - BrowserFragment calls MainActivity which calls BrowserFragment again - this is unnecessary.
     */
    fun updateCursorState() {
        val activity = activity as MainActivity
        val webView = webView
        // Bandaid null checks, underlying issue #249
        val enableCursor = webView != null &&
                webView.getUrl() != null &&
                !webView.getUrl()!!.contains("youtube.com/tv") &&
                context != null &&
                !context.isVoiceViewEnabled() // VoiceView has its own navigation controls.
        activity.setCursorEnabled(enableCursor)
    }

    fun moveCursor(direction: Direction) {
        cursor.moveCursor(direction)
    }

    fun stopMoving(direction: Direction) {
        cursor.stopMoving(direction)
    }

    fun setCursorEnabled(toEnable: Boolean) {
        cursor.visibility = if (toEnable) View.VISIBLE else View.GONE
    }

    override fun cursorHitEdge(edge: Edge) {
        val webView = webView ?: return

        when (edge) {
            Edge.TOP -> webView.flingScroll(0, -scrollVelocity)
            Edge.BOTTOM -> webView.flingScroll(0, scrollVelocity)
            Edge.LEFT -> webView.flingScroll(-scrollVelocity, 0)
            Edge.RIGHT -> webView.flingScroll(scrollVelocity, 0)
        }
    }
}

private class BrowserIWebViewCallback(
        private val browserFragment: BrowserFragment
): IWebView.Callback {

    private var fullscreenCallback: IWebView.FullscreenCallback? = null

    override fun onPageStarted(url: String) {}
    override fun onPageFinished(isSecure: Boolean) {}
    override fun onProgress(progress: Int) {}

    override fun onURLChanged(url: String) {}
    override fun onRequest(isTriggeredByUserGesture: Boolean) {}

    override fun countBlockedTracker() {}
    override fun resetBlockedTrackers() {}

    override fun onBlockingStateChanged(isBlockingEnabled: Boolean) {}

    override fun onLongPress(hitTarget: IWebView.HitTarget) {}

    override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
        fullscreenCallback = callback
        if (view == null) return

        with (browserFragment) {
            browserContainer.visibility = View.INVISIBLE

            val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            videoContainer.addView(view, params)
            videoContainer.visibility = View.VISIBLE
        }
    }

    override fun onExitFullScreen() {
        with (browserFragment) {
            videoContainer.removeAllViews()
            videoContainer.visibility = View.GONE

            browserContainer.visibility = View.VISIBLE
        }

        fullscreenCallback?.fullScreenExited()
        fullscreenCallback = null
    }
}
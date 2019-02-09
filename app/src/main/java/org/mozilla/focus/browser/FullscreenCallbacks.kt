/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.support.annotation.VisibleForTesting
import android.support.annotation.VisibleForTesting.PRIVATE
import android.support.v4.math.MathUtils
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_browser.*
import org.mozilla.focus.R
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.widget.OnInterceptTouchEventFrameLayout

/**
 * An [IWebView.Callback] that only handles the fullscreen related callbacks.
 */
open class FullscreenCallbacks(
    private val browserFragment: BrowserFragment,
    private val browserViewModel: BrowserViewModel,
    private val telemetryWrapper: TelemetryWrapper = TelemetryWrapper
) : IWebView.Callback {

    private var isInFullScreen = false
    private var exitOnScaleGestureListener: ExitFullscreenOnScaleGestureListener? = null

    // N.B: call this instead of using fullscreenContainer directly! It throws an NPE when used in
    // tests so we wrap it to modify the behavior in testing. Unfortunately, mocking did not work.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    open val fullscreenContainerOverride: OnInterceptTouchEventFrameLayout
        get() = browserFragment.fullscreenContainer

    override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
        if (view == null) return

        browserViewModel.fullscreenChanged(true)
        isInFullScreen = true
        exitOnScaleGestureListener = ExitFullscreenOnScaleGestureListener(callback, view)

        with(browserFragment) {
            webView?.setVisibility(View.GONE)

            val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            fullscreenContainerOverride.addView(view, params)
            fullscreenContainerOverride.visibility = View.VISIBLE

            // We intercept touch events from the fullscreen view's parent container because
            // listening for touch events on the fullscreen view didn't work for an unknown reason.
            val scaleGestureDetector = ScaleGestureDetector(view.context, exitOnScaleGestureListener)
            fullscreenContainerOverride.onInterceptTouchEventObserver = { scaleGestureDetector.onTouchEvent(it) }
        }

        Toast.makeText(view.context, R.string.fullscreen_toast_pinch_to_exit, Toast.LENGTH_SHORT).show()
    }

    override fun onExitFullScreen() {
        with(browserFragment) {
            webView?.setVisibility(View.VISIBLE)

            fullscreenContainerOverride.removeAllViews()
            fullscreenContainerOverride.visibility = View.GONE
            fullscreenContainerOverride.onInterceptTouchEventObserver = null
        }

        // This method may be erroneously called multiple times for each `onEnterFullScreen` (see
        // FirefoxWebView.goBack/goForward; this should be fixed with android components). To prevent
        // telemetry from being recorded more than once per full screen session, we only record
        // telemetry if this flag is true, which *only* happens if we've actually entered full screen.
        if (isInFullScreen) {
            isInFullScreen = false
            val wasExitedByScaleGesture = exitOnScaleGestureListener?.wasExitCalledByGesture!!
            telemetryWrapper.fullscreenExitEvent(wasExitedByScaleGesture)
        }
        browserViewModel.fullscreenChanged(false)
        exitOnScaleGestureListener = null
    }

    /**
     * A listener that will exit fullscreen if a scale gesture is used. This class assumes a new
     * instance will be created for each enter fullscreen event.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    class ExitFullscreenOnScaleGestureListener(
        private val callback: IWebView.FullscreenCallback,
        private val fullscreenView: View
    ) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        var wasExitCalledByGesture = false
            private set

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (fullscreenView.scaleX < 1) {
                // This breaks on the emulator: WebView does not redraw properly afterwards. Curiously,
                // the previous implementation which had `onExitFullScreen` call `fullScreenExited`,
                // resulting in `onExitFullScreen` being called twice, worked on the emulator.
                // However, this implementation is simpler and more correct (e.g. recording telemetry
                // is hard if it gets called twice) so we accept this caveat.
                wasExitCalledByGesture = true // Must be called before callback.
                callback.fullScreenExited()
            }
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = MathUtils.clamp(fullscreenView.scaleX * detector.scaleFactor, 0f, 1f)
            fullscreenView.scaleX = newScale
            fullscreenView.scaleY = newScale
            return true
        }
    }
}

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
import kotlinx.android.synthetic.main.fragment_browser.*
import org.mozilla.focus.iwebview.IWebView

/**
 * An [IWebView.Callback] that only handles the fullscreen related callbacks.
 */
class FullscreenCallbacks(
        private val browserFragment: BrowserFragment
) : IWebView.Callback {

    private var fullscreenCallback: IWebView.FullscreenCallback? = null
    private var exitOnScaleGestureListener: ExitFullscreenOnScaleGestureListener? = null

    override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
        if (view == null) return

        fullscreenCallback = callback
        exitOnScaleGestureListener = ExitFullscreenOnScaleGestureListener(callback, view)

        with(browserFragment) {
            callbacks?.onFullScreenChange(true)
            webView?.setVisibility(View.GONE)

            val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            fullscreenContainer.addView(view, params)
            fullscreenContainer.visibility = View.VISIBLE

            // We intercept touch events from the fullscreen view's parent container because
            // listening for touch events on the fullscreen view didn't work for an unknown reason.
            val scaleGestureDetector = ScaleGestureDetector(view.context, exitOnScaleGestureListener)
            fullscreenContainer.onInterceptTouchEventObserver = { scaleGestureDetector.onTouchEvent(it) }
        }
    }

    override fun onExitFullScreen() {
        with(browserFragment) {
            callbacks?.onFullScreenChange(false)
            webView?.setVisibility(View.VISIBLE)

            fullscreenContainer.removeAllViews()
            fullscreenContainer.visibility = View.GONE
            fullscreenContainer.onInterceptTouchEventObserver = null
        }

        // In my interpretation of the docs, fullScreenExited is supposed to be called when the
        // application wants to exit fullscreen, not when the application is exiting fullscreen so
        // this is unnecessary. In fact, it actually forces this method to be called twice. However,
        // without it, the emulator does not exit fullscreen correctly: the WebView never redraws.
        fullscreenCallback?.fullScreenExited()
        fullscreenCallback = null

        exitOnScaleGestureListener = null
    }

    @VisibleForTesting(otherwise = PRIVATE)
    class ExitFullscreenOnScaleGestureListener(
            private val callback: IWebView.FullscreenCallback,
            private val fullscreenView: View
    ) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (fullscreenView.scaleX < 1) {
                // This breaks on the emulator: WebView does not redraw properly afterwards. Curiously,
                // the previous implementation which had `onExitFullScreen` call `fullScreenExited`,
                // resulting in `onExitFullScreen` being called twice, worked on the emulator.
                // However, this implementation is simpler and more correct (e.g. recording telemetry
                // is hard if it gets called twice) so we accept this caveat.
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

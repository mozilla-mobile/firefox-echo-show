/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

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

    override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
        fullscreenCallback = callback
        if (view == null) return

        with(browserFragment) {
            callbacks?.onFullScreenChange(true)
            webView?.setVisibility(View.GONE)

            val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            videoContainer.addView(view, params)
            videoContainer.visibility = View.VISIBLE
        }
    }

    override fun onExitFullScreen() {
        with(browserFragment) {
            callbacks?.onFullScreenChange(false)
            webView?.setVisibility(View.VISIBLE)

            videoContainer.removeAllViews()
            videoContainer.visibility = View.GONE
        }

        fullscreenCallback?.fullScreenExited()
        fullscreenCallback = null
    }
}

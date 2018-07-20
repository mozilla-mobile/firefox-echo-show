/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.support.design.widget.AppBarLayout
import android.view.View
import org.mozilla.focus.browser.BrowserFragment

/** A controller for the [AppBarLayout] of the browser. */
class BrowserAppBarLayoutController(
        private val appBarLayout: AppBarLayout,
        private val appBarOverlay: View
) {

    fun initViews(getBrowserFragment: () -> BrowserFragment?) {
        appBarOverlay.setOnClickListener {
            appBarOverlay.visibility = View.GONE
            getBrowserFragment()?.setOverlayVisibleByUser(false, toAnimate = true)
        }
    }

    fun onHomeVisibilityChange(isHomeVisible: Boolean, isHomescreenOnStartup: Boolean) {
        // If this is the homescreen we show on startup, we want the user to be able to interact with
        // the toolbar and be unable to dismiss the home page (which has no content behind it). If
        // is another homescreen, we overlay the toolbar to prevent interacting with it and allow
        // dismissing, to show the web content, when clicked.
        appBarOverlay.visibility = if (isHomeVisible && !isHomescreenOnStartup) View.VISIBLE else View.GONE
    }

    fun onFullScreenChange(isFullscreen: Boolean) {
        appBarLayout.setExpanded(!isFullscreen, true) // Not expanded means hidden.
    }
}

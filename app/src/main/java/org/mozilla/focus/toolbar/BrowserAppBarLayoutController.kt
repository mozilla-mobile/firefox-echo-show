/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.view.ViewGroup
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import kotlinx.android.synthetic.main.activity_main.view.*
import mozilla.components.browser.toolbar.BrowserToolbar
import org.mozilla.focus.ext.forceExhaustive
import org.mozilla.focus.ext.updateLayoutParams
import org.mozilla.focus.utils.DeviceInfo
import org.mozilla.focus.utils.DeviceInfo.Model

private const val TOOLBAR_SCROLL_ENABLED_FLAGS = SCROLL_FLAG_SCROLL or
        SCROLL_FLAG_ENTER_ALWAYS or
        SCROLL_FLAG_SNAP or
        SCROLL_FLAG_EXIT_UNTIL_COLLAPSED

/** A view controller for the [AppBarLayout] and the [BrowserToolbar] it contains. */
class BrowserAppBarLayoutController(
    private val viewModel: BrowserAppBarViewModel,
    private val appBarLayout: AppBarLayout,
    private val deviceInfo: DeviceInfo
) : LifecycleObserver {

    fun init(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
        viewModel.isToolbarScrollEnabled.observe(lifecycleOwner, Observer {
            appBarLayout.appBarInnerContainer.setIsScrollEnabled(it!!)
        })

        viewModel.isAppBarHidden.observe(lifecycleOwner, Observer {
            // Visual glitches appear between the toolbar and the WebView if the WebView animates when exiting fullscreen
            // so we disable these animations as a simple solution: the animations had poor performance anyway.
            //
            // Note: expanded in setExpanded means hidden.
            appBarLayout.setExpanded(it!!, false)
        })

        maybeSetElevationToZero(appBarLayout)
    }

    /**
     * Sets the AppBar elevation to 0 to remove the shadow on the initial homescreen on some
     * devices; other devices are affected by a bug (#305) where setting elevation will cause WebView
     * videos to pause when the AppBar is scrolled off-screen: see R.id.appBarLayout for details.
     */
    private fun maybeSetElevationToZero(appBarLayout: AppBarLayout) {
        when (deviceInfo.deviceModel) {
            // These are known good devices.
            Model.FIRST_GEN, Model.SECOND_GEN -> {
                // targetElevation is the only simple way to change the elevation of the AppBar
                // (normal elevation uses a different system) so we'll use it while it's still working.
                @Suppress("DEPRECATION")
                appBarLayout.targetElevation = 0f
            }

            // The ES5 is a known bad device. We don't know if new devices will also share this bug
            // and we don't want to have to make a new build each time so we also disable elevation
            // on these new devices.
            Model.FIVE,
            Model.UNKNOWN -> { /* don't set elevation. */ }
        }.forceExhaustive
    }

    fun onHomeVisibilityChange(isHomeVisible: Boolean) {
        viewModel.onNavigationOverlayVisibilityChange(isHomeVisible)
    }
}

private fun ViewGroup.setIsScrollEnabled(isScrollEnabled: Boolean) {
    updateLayoutParams {
        val layoutParams = it as AppBarLayout.LayoutParams

        // The only way to update scrolling enabled is to change the layoutParams scrollFlags x_x
        layoutParams.scrollFlags = if (isScrollEnabled) TOOLBAR_SCROLL_ENABLED_FLAGS else 0
    }
}

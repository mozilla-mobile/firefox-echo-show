/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.Lifecycle.Event.ON_START
import android.arch.lifecycle.Lifecycle.Event.ON_STOP
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.OnLifecycleEvent
import android.support.annotation.VisibleForTesting
import android.support.annotation.VisibleForTesting.PRIVATE
import android.support.design.widget.AppBarLayout
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import android.view.accessibility.AccessibilityManager
import mozilla.components.browser.toolbar.BrowserToolbar
import org.mozilla.focus.ext.getAccessibilityManager
import org.mozilla.focus.ext.isVoiceViewEnabled
import org.mozilla.focus.ext.updateLayoutParams

private const val TOOLBAR_SCROLL_ENABLED_FLAGS = SCROLL_FLAG_SCROLL or
        SCROLL_FLAG_ENTER_ALWAYS or
        SCROLL_FLAG_SNAP or
        SCROLL_FLAG_EXIT_UNTIL_COLLAPSED

/** A view controller for the [AppBarLayout] and the [BrowserToolbar] it contains. */
class BrowserAppBarLayoutController(
    private val viewModel: BrowserAppBarViewModel,
    private val appBarLayout: AppBarLayout,
    private val toolbar: BrowserToolbar
) : AccessibilityManager.TouchExplorationStateChangeListener, LifecycleObserver {

    private val context = appBarLayout.context

    fun init(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
        viewModel.isToolbarScrollEnabled.observe(lifecycleOwner, Observer {
            toolbar.setIsScrollEnabled(it!!)
        })
    }

    @VisibleForTesting(otherwise = PRIVATE)
    @OnLifecycleEvent(ON_START)
    fun onStart() {
        context.getAccessibilityManager().addTouchExplorationStateChangeListener(this)

        // The state may have changed while the listener was unattached: refresh it.
        viewModel.setIsVoiceViewEnabled(context.isVoiceViewEnabled())
    }

    @VisibleForTesting(otherwise = PRIVATE)
    @OnLifecycleEvent(ON_STOP)
    fun onStop() {
        context.getAccessibilityManager().removeTouchExplorationStateChangeListener(this)
    }

    fun onHomeVisibilityChange(isHomeVisible: Boolean) {
        viewModel.setIsNavigationOverlayVisible(isHomeVisible)
    }

    override fun onTouchExplorationStateChanged(enabled: Boolean) {
        viewModel.setIsVoiceViewEnabled(enabled) // touch exploration state = VoiceView
    }

    fun onFullScreenChange(isFullscreen: Boolean) {
        appBarLayout.setExpanded(!isFullscreen, true) // Not expanded means hidden.
    }
}

private fun BrowserToolbar.setIsScrollEnabled(isScrollEnabled: Boolean) {
    updateLayoutParams {
        val layoutParams = it as AppBarLayout.LayoutParams

        // The only way to update scrolling enabled is to change the layoutParams scrollFlags x_x
        layoutParams.scrollFlags = if (isScrollEnabled) TOOLBAR_SCROLL_ENABLED_FLAGS else 0
    }
}

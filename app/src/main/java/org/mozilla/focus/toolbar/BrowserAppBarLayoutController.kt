/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Lifecycle.Event.ON_START
import android.arch.lifecycle.Lifecycle.Event.ON_STOP
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
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
    private val appBarLayout: AppBarLayout,
    private val toolbar: BrowserToolbar
) : AccessibilityManager.TouchExplorationStateChangeListener, LifecycleObserver {

    private val context = appBarLayout.context
    private var isHomeVisible = false

    fun init(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(ON_START)
    private fun onStart() {
        context.getAccessibilityManager().addTouchExplorationStateChangeListener(this)
        updateCanScroll(isHomeVisible = isHomeVisible, isVoiceViewEnabled = context.isVoiceViewEnabled())
    }

    @OnLifecycleEvent(ON_STOP)
    private fun onStop() {
        context.getAccessibilityManager().removeTouchExplorationStateChangeListener(this)
    }

    private fun updateCanScroll(isHomeVisible: Boolean, isVoiceViewEnabled: Boolean) {
        val canScroll = !isHomeVisible && !isVoiceViewEnabled
        toolbar.setIsScrollEnabled(canScroll)
    }

    fun onHomeVisibilityChange(isHomeVisible: Boolean) {
        updateCanScroll(isHomeVisible = isHomeVisible, isVoiceViewEnabled = context.isVoiceViewEnabled())
        this.isHomeVisible = isHomeVisible
    }

    override fun onTouchExplorationStateChanged(enabled: Boolean) { // touch exploration state = VoiceView
        updateCanScroll(isHomeVisible = isHomeVisible, isVoiceViewEnabled = enabled)
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

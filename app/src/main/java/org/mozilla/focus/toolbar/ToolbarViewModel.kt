/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import org.mozilla.focus.ActiveScreen.NAVIGATION_OVERLAY
import org.mozilla.focus.ScreenRepo
import org.mozilla.focus.telemetry.TelemetryWrapper

/**
 * Represents view state for our wrapper around
 * [mozilla.components.browser.toolbar.BrowserToolbar] and provides an interface
 * for its views to notify the model of UI events.
 */
class ToolbarViewModel(
    private val screenRepo: ScreenRepo
) : ViewModel() {

    @UiThread // via setActiveScreen
    fun homeButtonClick() {
        if (!screenRepo.activeScreen.value!!.isNavigationOverlay) {
            screenRepo.setActiveScreen(NAVIGATION_OVERLAY)
            TelemetryWrapper.toolbarEvent(ToolbarEvent.HOME, null)
        }
    }
}

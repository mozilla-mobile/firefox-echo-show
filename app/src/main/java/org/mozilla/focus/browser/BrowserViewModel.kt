/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import org.mozilla.focus.ActiveScreen.BROWSER
import org.mozilla.focus.ActiveScreen.NAVIGATION_OVERLAY_ON_STARTUP
import org.mozilla.focus.ScreenRepo

/**
 * Represents possible view state for [org.mozilla.focus.browser.BrowserFragment] and provides an interface for its
 * views to notify the model of UI events.
*/
class BrowserViewModel(
    private val screenRepo: ScreenRepo
) : ViewModel() {

    val activeScreen = screenRepo.activeScreen

    @UiThread // via setActiveScreen
    fun startupUrlSet() {
        screenRepo.setActiveScreen(NAVIGATION_OVERLAY_ON_STARTUP)
    }

    @UiThread // via setActiveScreen
    fun loadUrlCalled() {
        screenRepo.setActiveScreen(BROWSER)
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import org.mozilla.focus.ActiveScreen.BROWSER
import org.mozilla.focus.ScreenRepo

/**
 * Represents possible view state for [org.mozilla.focus.home.NavigationOverlayFragment] and provides an interface for its
 * views to notify the model of UI events.
 */
class NavigationOverlayViewModel(
    private val screenRepo: ScreenRepo
) : ViewModel() {

    @UiThread // via setActiveScreen
    fun dismissOverlayClick() {
        screenRepo.setActiveScreen(BROWSER)
    }
}

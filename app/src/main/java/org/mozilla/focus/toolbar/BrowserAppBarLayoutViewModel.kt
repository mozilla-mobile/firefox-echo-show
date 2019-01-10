/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import org.mozilla.focus.ScreenRepo

/**
 * Represents view state for our wrapper around
 * [android.support.design.widget.AppBarLayout] and provides an interface
 * for its views to notify the model of UI events.
 */
class BrowserAppBarLayoutViewModel(
    screenRepo: ScreenRepo
) : ViewModel() {

    val isNavigationOverlayVisible: LiveData<Boolean> = Transformations.map(screenRepo.activeScreen) {
        it.isNavigationOverlay
    }
}

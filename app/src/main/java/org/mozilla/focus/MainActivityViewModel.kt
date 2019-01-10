/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.arch.lifecycle.ViewModel

/**
 * Represents possible view state for [org.mozilla.focus.MainActivity] and provides an interface for its
 * views to notify the model of UI events.
 */
class MainActivityViewModel(
    screenRepo: ScreenRepo
) : ViewModel() {

    val activeScreen = screenRepo.activeScreen
}

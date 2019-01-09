/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.UiThread

/**
 * A repository related to what is displayed on the screen.
 */
class ScreenRepo {

    private val _activeScreen = MutableLiveData<ActiveScreen>()
    val activeScreen: LiveData<ActiveScreen> = _activeScreen

    @UiThread // for simplicity: UI updates must occur on the UI thread so we can expect this of callers too.
    fun setActiveScreen(activeScreen: ActiveScreen) {
        _activeScreen.value = activeScreen
    }
}

/**
 * The states in the app where something is active on the screen.
 */
enum class ActiveScreen {
    BROWSER,

    NAVIGATION_OVERLAY_ON_STARTUP,
    NAVIGATION_OVERLAY;

    val isNavigationOverlay: Boolean get() = this == NAVIGATION_OVERLAY_ON_STARTUP || this == NAVIGATION_OVERLAY
}

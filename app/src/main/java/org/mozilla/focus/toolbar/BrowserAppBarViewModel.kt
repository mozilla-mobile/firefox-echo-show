/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import org.mozilla.focus.architecture.FrameworkRepo
import org.mozilla.focus.ext.LiveDataCombiners
import org.mozilla.focus.ext.map
import org.mozilla.focus.session.SessionRepo

/**
 * The view state, and UI event callbacks, for the app bar layout.
 */
class BrowserAppBarViewModel(
    frameworkRepo: FrameworkRepo,
    sessionRepo: SessionRepo
) : ViewModel() {

    private val isNavigationOverlayVisible = MutableLiveData<Boolean>()

    val isToolbarScrollEnabled = LiveDataCombiners.combineLatest(
        frameworkRepo.isVoiceViewEnabled,
        isNavigationOverlayVisible
    ) { isVoiceViewEnabled, isNavigationOverlayVisible ->
        !isVoiceViewEnabled && !isNavigationOverlayVisible
    }

    val isAppBarHidden = sessionRepo.isFullscreen.map { !it }

    // TODO: this property should be reactively pushed from the model.
    @UiThread
    fun onNavigationOverlayVisibilityChange(isVisible: Boolean) {
        isNavigationOverlayVisible.value = isVisible
    }
}

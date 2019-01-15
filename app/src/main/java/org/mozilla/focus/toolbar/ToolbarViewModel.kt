/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread

/**
 * View state, and callback interface for UI events, for the android-components toolbar.
 */
class ToolbarViewModel : ViewModel() {

    private val _isToolbarImportantForAccessibility = MutableLiveData<Boolean>()
    val isToolbarImportantForAccessibility: LiveData<Boolean> = _isToolbarImportantForAccessibility

    // TODO: push this value reactively from the model.
    @UiThread // for simplicity.
    fun onNavigationOverlayVisibilityChange(isVisible: Boolean, isNavigationOverlayOnStartup: Boolean) {
        // The navigation overlay covers the screen with a semi opaque overlay, like a dialog:
        // nothing should be accessible below it including this toolbar.
        val isToolbarNotImportantForAccessibility = isVisible && !isNavigationOverlayOnStartup
        _isToolbarImportantForAccessibility.value = !isToolbarNotImportantForAccessibility
    }
}

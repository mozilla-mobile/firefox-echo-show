/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.architecture

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.UiThread
import android.view.accessibility.AccessibilityManager

/**
 * A model to hold state related to the Android framework.
 */
class FrameworkRepo : AccessibilityManager.TouchExplorationStateChangeListener {

    private val _isVoiceViewEnabled = MutableLiveData<Boolean>()
    val isVoiceViewEnabled: LiveData<Boolean> = _isVoiceViewEnabled

    fun init(accessibilityManager: AccessibilityManager) {
        // We call the listener directly to set the initial state.
        accessibilityManager.addTouchExplorationStateChangeListener(this)
        onTouchExplorationStateChanged(accessibilityManager.isTouchExplorationEnabled)
    }

    @UiThread // for simplicity: listener should be called from UI thread anyway.
    override fun onTouchExplorationStateChanged(isEnabled: Boolean) {
        _isVoiceViewEnabled.value = isEnabled // Touch exploration state == VoiceView.
    }
}

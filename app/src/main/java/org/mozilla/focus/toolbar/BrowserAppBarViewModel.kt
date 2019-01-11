/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * The view state, and UI event callbacks, for the app bar layout.
 */
class BrowserAppBarViewModel : ViewModel() {

    private var _isToolbarScrollEnabled = MutableLiveData<Boolean>()
    val isToolbarScrollEnabled: LiveData<Boolean> = _isToolbarScrollEnabled

    // The initial values are unimportant because they're immediately updated.
    // TODO: these properties should be reactively pushed from the model.
    private var isNavigationOverlayVisible: Boolean by ToolbarScrollAffectingProperty(false)
    private var isVoiceViewEnabled: Boolean by ToolbarScrollAffectingProperty(false)

    @UiThread
    fun setIsNavigationOverlayVisible(isVisible: Boolean) {
        isNavigationOverlayVisible = isVisible
    }

    @UiThread
    fun setIsVoiceViewEnabled(isEnabled: Boolean) {
        isVoiceViewEnabled = isEnabled
    }

    /**
     * A delegated property to be used for properties whose value affect the [isToolbarScrollEnabled] property.
     * This is used to reduce code duplication over multiple observables.
     */
    private inner class ToolbarScrollAffectingProperty(initialValue: Boolean) : ObservableProperty<Boolean>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: Boolean, newValue: Boolean) {
            // For simplicity, update on the UI thread. This property affects UI so it should be UI thread anyway.
            _isToolbarScrollEnabled.value = !isNavigationOverlayVisible && !isVoiceViewEnabled
        }
    }
}

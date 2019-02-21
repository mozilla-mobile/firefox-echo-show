/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData

/**
 * A model for the currently active [Session]s.
 */
class SessionRepo {

    private val _isFullscreen = MutableLiveData<Boolean>().apply {
        value = false // the browser will never start fullscreen.
    }
    val isFullscreen: LiveData<Boolean> = _isFullscreen

    // We could import SessionObserverHelper from FFTV but it's non-trivial since we're not using components
    // yet: instead, we just receive events from this callback.
    fun fullscreenChange(isFullscreen: Boolean) {
        _isFullscreen.value = isFullscreen
    }
}

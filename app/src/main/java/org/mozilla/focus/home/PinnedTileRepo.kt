/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import mozilla.components.support.base.observer.Consumable

/**
 * A repository encapsulating possible states of the pinned tiles.
 */
class PinnedTileRepo {

    private val _googleSearchEvents = MutableLiveData<Consumable<GoogleSearchFocusRequestEvent>>()
    val googleSearchEvents: LiveData<Consumable<GoogleSearchFocusRequestEvent>> = _googleSearchEvents

    fun googleSearchFocusRequest() {
        _googleSearchEvents.postValue(Consumable.from(GoogleSearchFocusRequestEvent))
    }
}

object GoogleSearchFocusRequestEvent

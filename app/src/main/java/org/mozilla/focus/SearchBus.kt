/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.net.Uri
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import java.lang.ref.WeakReference

/**
 * A [UrlSearcher] can register to observe this, allowing other objects to push
 * search requests to it indirectly.
 *
 * **Note that only one [UrlSearcher] can observe this at any given time**.
 * Subsequent calls to [observe] will replace the previous reference.
 */
object SearchBus {

    // Must be a WeakReference because this object can outlive any Activities
    // and Fragments, and so cannot hold strong references to them
    private var searcherRef: WeakReference<UrlSearcher>? = null

    fun observe(urlSearcher: UrlSearcher) {
        searcherRef = WeakReference(urlSearcher)
    }

    fun push(url: Uri) {
        searcherRef?.get()?.onTextInputUrlEntered(url.toString(),
                InlineAutocompleteEditText.AutocompleteResult.emptyResult())
    }
}

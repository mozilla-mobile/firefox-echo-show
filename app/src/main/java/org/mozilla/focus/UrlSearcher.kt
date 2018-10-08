/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import mozilla.components.ui.autocomplete.InlineAutocompleteEditText

interface UrlSearcher {
    fun onTextInputUrlEntered(
        urlStr: String,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult
    )
}

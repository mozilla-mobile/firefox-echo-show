/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText.AutocompleteResult
import org.mozilla.focus.UrlSearcher

/**
 * A [TextView.OnEditorActionListener] that will make a search using the default engine on submit.
 */
class DefaultSearchOnEditorActionListener(
    private val urlSearcher: UrlSearcher
) : TextView.OnEditorActionListener {
    override fun onEditorAction(textView: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (!actionId.isTerminalEvent()) return false

        val query = textView.text.toString()
        urlSearcher.onTextInputUrlEntered(query, AutocompleteResult.emptyResult())
        return true
    }

    // Different devices emit different events when the soft keyboard search
    // button is pressed
    private fun Int.isTerminalEvent(): Boolean {
        return this == EditorInfo.IME_ACTION_NEXT || // Amazon Connect
            this == EditorInfo.IME_ACTION_DONE // Emulated 10.1 WXGA Tablet API 22
    }
}

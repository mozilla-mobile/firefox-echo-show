/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import mozilla.components.support.ktx.android.content.systemService
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.R

private const val HIDDEN_EDIT_TEXT_ID = R.id.hiddenEditText

/**
 * Used to make an EditText hidden from the user while still having a
 * visibility of [View.VISIBLE].
 *
 * Views that are [View.INVISIBLE] or [View.GONE] cannot accept focus.
 * These functions can be used when input needs to be captured using the device's
 * soft keyboard, without any other visible UI. This may be accomplished by
 * having the EditText request focus and programmatically presenting the
 * keyboard, then querying the view for resulting text.
 */
object HiddenEditTextManager {

    /**
     * Attaches a hidden EditText to a ViewGroup
     */
    fun attach(viewGroupToAttachTo: ViewGroup) {
        val editText = EditText(viewGroupToAttachTo.context).apply {
            setImeActionLabel(null, EditorInfo.IME_ACTION_SEARCH)
            // Elevation should be lower than the rest of the ViewGroup for this to
            // remain hidden.
            elevation = -8f
            id = HIDDEN_EDIT_TEXT_ID
            inputType = InputType.TYPE_CLASS_TEXT
            hint = viewGroupToAttachTo.context.getString(R.string.google_search_hint_text)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        viewGroupToAttachTo.addView(editText)
        editText.layoutParams = editText.layoutParams.apply {
            // Height and width are set to 1 to prevent the EditText from
            // potentially being longer than the tile and peeking out
            // around the sides.
            height = 1
            width = 1
        }
    }

    /**
     * Removes any view added by [attach]
     */
    fun removeIfPresent(viewGroup: ViewGroup) {
        viewGroup.getEditText()?.let { viewGroup.removeView(it) }
    }

    /**
     * Opens soft keyboard and causes the hidden EditText to request focus.
     *
     * Should be used on a view that has already been passed to
     * [HiddenEditTextManager.attach]
     */
    fun openSoftKeyboard(view: View) {
        fun showKeyboard() {
            view.context?.systemService<InputMethodManager>(Context.INPUT_METHOD_SERVICE)
                    ?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }

        val editText = view.getEditText()

        editText?.setText("")
        // Programmatically requesting focus on the device does not open
        // the soft keyboard for an unknown reason, so we need to open
        // the keyboard manually in addition to requesting focus
        showKeyboard()
        editText?.requestFocus()
    }

    /**
     * Sets [executeSearch] as a listener to fire when the keyboard go button is
     * pressed.
     *
     * Should be used on a view that has already been passed to
     * [HiddenEditTextManager.attach]
     */
    fun setKeyboardListener(view: View, executeSearch: ExecuteSearch) {
        val editText = view.getEditText()
        editText?.setOnEditorActionListener(SearchOnGoEditorActionListener(editText, executeSearch))
    }

    private fun View.getEditText() = this.findViewById<EditText?>(HIDDEN_EDIT_TEXT_ID)
}

private class SearchOnGoEditorActionListener(
    private val searchField: EditText,
    private val executeSearch: ExecuteSearch
) : TextView.OnEditorActionListener {

    // Different devices emit different events when the soft keyboard search
    // button is pressed
    fun Int.isTerminalEvent(): Boolean {
        return this == EditorInfo.IME_ACTION_NEXT || // Amazon Connect
                this == EditorInfo.IME_ACTION_DONE   // Emulated 10.1 WXGA Tablet API 22
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (!actionId.isTerminalEvent()) return false
        executeSearch.invoke(
                searchField.text.toString(),
                InlineAutocompleteEditText.AutocompleteResult.emptyResult()
        )
        return true
    }
}

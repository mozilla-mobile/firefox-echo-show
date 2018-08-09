package org.mozilla.focus.utils

import android.content.Context
import android.content.ContextWrapper
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.MainActivity
import org.mozilla.focus.R
import java.lang.ref.WeakReference

/**
 * Attaches a hidden EditText to a ViewGroup
 *
 * The function is used when input needs to be captured using the native keyboard,
 * without any other visible UI. This may be accomplished by having the EditText
 * request focus and programmatically presenting the keyboard, then querying the
 * view for resulting text.
 */
fun attachHiddenEditText(group: ViewGroup, hintText: String) {
    EditText(group.context).apply {
        setImeActionLabel(null, EditorInfo.IME_ACTION_SEARCH)
        group.addView(this)
        val params = layoutParams
        params.height = 1
        params.width = 1
        layoutParams = params
        // Elevation should be lower than the rest of the ViewGroup for this to remain hidden
        elevation = -8f
        id = R.id.hiddenEditText
        inputType = InputType.TYPE_CLASS_TEXT
        hint = hintText
    }
}

/**
 * Removes any view added by [attachHiddenEditText]
 */
fun removeHiddenEditText(group: View) {
    if (group !is ViewGroup) return
    group.findViewById<EditText>(R.id.hiddenEditText)?.let { group.removeView(it) }
}

/**
 * Programmatically opens a soft keyboard and focuses a descendant of [v] with
 * the ID of [R.id.hiddenEditText]
 */
class HiddenEditTextClickListener : View.OnClickListener {
    override fun onClick(parent: View?) {
        parent ?: return

        val editText = parent.findViewById<EditText>(R.id.hiddenEditText)
        val showKeyboard = {
            (parent.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }
        val mainActivity: MainActivity? = editText.let {
            var context = it.context
            while (true) {
                when (context) {
                    is MainActivity -> return@let context
                    is ContextWrapper -> context = context.baseContext
                    else -> return@let null
                }
            }
            @Suppress("UNREACHABLE_CODE")
            return@let null
        }

        showKeyboard()
        editText.setText("")
        editText.requestFocus()
        editText.setOnEditorActionListener(NavigateOnGo(mainActivity, editText))
    }
}

private class NavigateOnGo(activity: MainActivity?, searchField: EditText) : TextView.OnEditorActionListener {

    val activityRef = WeakReference(activity)
    val searchRef = WeakReference(searchField)

    // Different devices emit different events when the soft keyboard search button is pressed
    fun Int.isTerminalEvent(): Boolean {
        return this == EditorInfo.IME_ACTION_NEXT || this == EditorInfo.IME_ACTION_DONE
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (!actionId.isTerminalEvent() || activityRef.get() == null ||
                searchRef.get() == null) return false
        activityRef.get()?.onTextInputUrlEntered(
                urlStr = searchRef.get()?.text.toString(),
                autocompleteResult = InlineAutocompleteEditText.AutocompleteResult.emptyResult()
        )
        return true
    }
}

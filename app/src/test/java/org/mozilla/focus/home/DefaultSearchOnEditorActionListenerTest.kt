/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.focus.UrlSearcher
import org.mozilla.focus.helpers.anyNonNull

class DefaultSearchOnEditorActionListenerTest {

    private lateinit var listener: DefaultSearchOnEditorActionListener
    private lateinit var urlSearcher: UrlSearcher

    @Before
    fun setUp() {
        urlSearcher = mock(UrlSearcher::class.java)
        listener = DefaultSearchOnEditorActionListener(urlSearcher)
    }

    @Test
    fun `WHEN the listener is called with next THEN a search is made`() {
        listener.onEditorActionWithId(EditorInfo.IME_ACTION_NEXT)
        verify(urlSearcher, times(1)).onTextInputUrlEntered(anyString(), anyNonNull())
    }

    @Test
    fun `WHEN the listener is called with done THEN a search is made`() {
        listener.onEditorActionWithId(EditorInfo.IME_ACTION_DONE)
        verify(urlSearcher, times(1)).onTextInputUrlEntered(anyString(), anyNonNull())
    }

    @Test
    fun `WHEN the listener is called with non-done or next IME actions THEN a search is not made`() {
        arrayOf(
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.IME_ACTION_PREVIOUS,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_UNSPECIFIED
        ).forEach { imeActionId ->
            println("Executed imeActionId $imeActionId")
            listener.onEditorActionWithId(imeActionId)
            verify(urlSearcher, never()).onTextInputUrlEntered(anyString(), anyNonNull())
        }
    }

    private fun DefaultSearchOnEditorActionListener.onEditorActionWithId(actionId: Int) {
        val textView = mock(TextView::class.java).also {
            `when`(it.text).thenReturn("TextView text")
        }

        listener.onEditorAction(textView, actionId, mock(KeyEvent::class.java))
    }
}

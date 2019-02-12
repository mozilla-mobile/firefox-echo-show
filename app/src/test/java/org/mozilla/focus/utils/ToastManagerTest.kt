/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ToastManagerTest {

    private lateinit var context: Context

    private val lastToastText: String get() = ShadowToast.getTextOfLatestToast()
    private val lastToastTextLowercase: String get() = lastToastText.toLowerCase(Locale.ENGLISH)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun `GIVEN showUnpinnedToast creates the toast text with a string substitution WHEN showUnpinnedToast is called THEN the toast text does not contain the string substitution template literal`() {
        ToastManager.showUnpinnedToast(context)
        assertToastDoesNotContainStringSubstitutionTemplate()
    }

    @Test
    fun `WHEN showUnpinnedToast is called THEN the toast text contains "removed"`() {
        ToastManager.showUnpinnedToast(context)
        assertToastContains("removed")
    }

    @Test
    fun `GIVEN showPinnedToast creates the toast text with a string substitution WHEN showPinnedToast is called THEN the toast text does not contain the string substitution template literal`() {
        ToastManager.showPinnedToast(context)
        assertToastDoesNotContainStringSubstitutionTemplate()
    }

    @Test
    fun `WHEN showPinnedToast is called THEN it contains "pinned" text`() {
        ToastManager.showPinnedToast(context)
        assertToastContains("pinned")
    }

    @Test // we test for "pinned" elsewhere but that string can be "unpinned"
    fun `WHEN showPinnedToast is called THEN it does not contain "unpinned" text`() {
        ToastManager.showPinnedToast(context)
        assertFalse(lastToastTextLowercase, lastToastTextLowercase.contains("unpinned"))
    }

    @Test
    fun `WHEN showToast is called with a String THEN a toast is shown with the string`() {
        val expected = "this String should appear in the toast 네?"
        ToastManager.showToast(expected, context)
        assertEquals(expected, lastToastText)
    }

    @Test
    fun `WHEN showToast is called with a res ID THEN a toast is shown with the string for the res ID`() {
        val input = R.string.firefox_brand_name
        val expected = context.getString(input)
        ToastManager.showToast(input, context)
        assertEquals(expected, lastToastText)
    }

    private fun assertToastDoesNotContainStringSubstitutionTemplate() {
        // Substitution template format is like %1$s
        assertFalse(lastToastText, lastToastText.contains("\$s"))
    }

    private fun assertToastContains(expectedSubStr: String) {
        assertTrue(lastToastTextLowercase, lastToastTextLowercase.contains(expectedSubStr))
    }
}

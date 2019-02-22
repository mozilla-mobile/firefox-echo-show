/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.ext.toSafeIntent
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val TEST_URL = "https://github.com/mozilla-mobile/focus-android"

@RunWith(RobolectricTestRunner::class)
class IntentValidatorTest {

    private val context: Context get() = RuntimeEnvironment.application

    @Test
    fun testViewIntent() {
        val expectedUrl = TEST_URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(expectedUrl)).toSafeIntent()
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    /** In production we see apps send VIEW intents without an URL. (Focus #1373) */
    @Test
    fun testViewIntentWithNullURL() {
        val intent = Intent(Intent.ACTION_VIEW, null).toSafeIntent()
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertNull(actual)
    }

    @Test
    fun testCustomTabIntent() {
        val expectedUrl = TEST_URL
        val intent = CustomTabsIntent.Builder()
                .setToolbarColor(Color.GREEN)
                .addDefaultShareMenuItem()
                .build()
                .intent
                .setData(Uri.parse(expectedUrl))
                .toSafeIntent()
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    @Test
    fun testViewIntentFromHistoryIsIgnored() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TEST_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        }.toSafeIntent()

        assertNull(IntentValidator(intent).getUriToOpen(context))
    }

    @Test
    fun testIntentNotValidIfWeAreRestoring() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TEST_URL)).toSafeIntent()
        assertNull(IntentValidator(intent).getUriToOpen(context, Bundle()))
    }

    @Test
    fun testShareIntentViaNewIntent() {
        val expectedUrl = TEST_URL
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, expectedUrl)
        }.toSafeIntent()
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    @Test
    fun testShareIntentWithTextViaNewIntent() {
        val expectedText = "Hello World Firefox TV"
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, expectedText)
        }.toSafeIntent()

        val searchUrl = IntentValidator(intent).getUriToOpen(context)
        expectedText.split(" ").forEach {
            assertTrue("Expected search url to contain $it", searchUrl!!.contains(it))
        }
    }
}

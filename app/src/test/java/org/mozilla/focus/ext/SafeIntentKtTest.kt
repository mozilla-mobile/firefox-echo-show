/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val TEST_URL = "https://github.com/mozilla-mobile/focus-android"

@RunWith(RobolectricTestRunner::class)
class SafeIntentKtTest {

    private val context: Context get() = RuntimeEnvironment.application

    @Test
    fun `WHEN receiving an intent with a null action THEN a null uri is returned`() {
        val intent = Intent(null, TEST_URL.toUri()).toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertNull(actual)
    }

    @Test
    fun `WHEN receiving a view intent with a valid uri THEN the uri is returned`() {
        val expectedUrl = TEST_URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(expectedUrl)).toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    /** In production we see apps send VIEW intents without an URL. (Focus #1373) */
    @Test
    fun `WHEN receiving a view intent with a null uri THEN a null uri is returned`() {
        val intent = Intent(Intent.ACTION_VIEW, null).toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertNull(actual)
    }

    @Test
    fun `WHEN receiving a view intent with a blank uri THEN a null uri is returned`() {
        arrayOf(
            "",
            "     "
        ).forEachIndexed { i, blankStr ->
            val intent = Intent(Intent.ACTION_VIEW, blankStr.toUri()).toSafeIntent()
            val actual = intent.getUriToOpen(context)
            assertNull("index $i", actual)
        }
    }

    @Test
    fun `WHEN receiving a custom tabs intent with a valid uri THEN the uri is returned`() {
        // Custom tab intents are view intents. Since FFES doesn't support custom tabs, we don't have code
        // to handle them specially and we handle them like view intents.
        val expectedUrl = TEST_URL
        val intent = CustomTabsIntent.Builder()
            .setToolbarColor(Color.GREEN)
            .addDefaultShareMenuItem()
            .build()
            .intent
            .setData(Uri.parse(expectedUrl))
            .toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    @Test
    fun `WHEN receiving a view intent with the launched from history flag THEN the uri is returned`() {
        val expectedUrl = TEST_URL
        val intent = Intent(Intent.ACTION_VIEW, expectedUrl.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        }.toSafeIntent()

        assertEquals(expectedUrl, intent.getUriToOpen(context))
    }

    @Test
    fun `WHEN receiving a send intent with a valid uri THEN the uri is returned`() {
        val expectedUrl = TEST_URL
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, expectedUrl)
        }.toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    @Test
    fun `WHEN receiving a send intent with text THEN a search uri is returned`() {
        val expectedText = "Hello World Firefox TV"
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, expectedText)
        }.toSafeIntent()

        val searchUrl = intent.getUriToOpen(context)
        expectedText.split(" ").forEach {
            assertTrue("Expected search url to contain $it", searchUrl!!.contains(it))
        }
    }

    @Test
    fun `WHEN receiving a share intent with no text extra THEN a null uri is returned`() {
        val intent = Intent(Intent.ACTION_SEND).toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertNull(actual)
    }

    @Test
    fun `WHEN receiving a share intent with null text THEN a null uri is returned`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, null as String?)
        }.toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertNull(actual)
    }

    @Test
    fun `WHEN receiving a share intent with blank text THEN a null uri is returned`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "    ")
        }.toSafeIntent()
        val actual = intent.getUriToOpen(context)
        assertNull(actual)
    }

    @Test
    fun `WHEN receiving an unrecognized intent THEN a null uri is returned`() {
        arrayOf(
            Intent.ACTION_MAIN,
            Intent.ACTION_SENDTO,
            Intent.ACTION_SEND_MULTIPLE,
            Intent.ACTION_CALL
        ).forEach {
            val intent = Intent(it, TEST_URL.toUri()).toSafeIntent()
            val actual = intent.getUriToOpen(context)
            assertNull("Expeceted null for action $it", actual)
        }
    }
}

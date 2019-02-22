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
    fun `WHEN receiving a view intent with a valid uri THEN the uri is returned`() {
        val expectedUrl = TEST_URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(expectedUrl)).toSafeIntent()
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    /** In production we see apps send VIEW intents without an URL. (Focus #1373) */
    @Test
    fun `WHEN receiving a view intent with a null uri THEN a null uri is returned`() {
        val intent = Intent(Intent.ACTION_VIEW, null).toSafeIntent()
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertNull(actual)
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
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    @Test
    fun `WHEN receiving a view intent with the launched from history flag THEN a null uri is returned`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TEST_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        }.toSafeIntent()

        assertNull(IntentValidator(intent).getUriToOpen(context))
    }

    @Test
    fun `WHEN restoring state THEN a null uri is returned`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TEST_URL)).toSafeIntent()
        assertNull(IntentValidator(intent).getUriToOpen(context, Bundle()))
    }

    @Test
    fun `WHEN receiving a send intent with a valid uri THEN the uri is returned`() {
        val expectedUrl = TEST_URL
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, expectedUrl)
        }.toSafeIntent()
        val actual = IntentValidator(intent).getUriToOpen(context)
        assertEquals(expectedUrl, actual)
    }

    @Test
    fun `WHEN receiving a send intent with text THEN a search uri is returned`() {
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

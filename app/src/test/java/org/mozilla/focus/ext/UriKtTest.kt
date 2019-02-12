/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriKtTest {

    @Test
    fun `WHEN truncatedHost is called on a null host THEN it returns null`() {
        val uri = httpsUriBuilder()
            .path("mozilla/en-US")
            .authority(null)
            .build()
        assertNull(uri.host)

        assertNull(uri.truncatedHost())
    }

    @Test
    fun `WHEN truncatedHost is called on an empty host THEN it returns the host`() {
        val hosts = arrayOf(
            "",
            "    "
        )
        val uris = hosts.map {
            httpsUriBuilder()
                .path("mozilla/en-US")
                .authority(it)
                .build()
        }
        hosts.zip(uris).forEachIndexed { i, (host, uri) ->
            val assertMsg = "index: $i" // hosts contain spaces so we must print index to disambiguate in loop
            assertEquals(assertMsg, host, uri.host) // assert init code is correct.

            assertEquals(assertMsg, host, uri.truncatedHost()) // assert method is correct.
        }
    }

    @Test
    fun `WHEN truncatedHost is called on a valid Uri THEN the truncated host is returned`() {
        arrayOf(
            Pair("https://tomshardware.co.uk", "tomshardware.co.uk"),
            Pair("https://mail.google.com/emails", "google.com")
        ).map { (uriStr, expectedStr) ->
            Pair(uriStr.toUri()!!, expectedStr)
        }.forEach { (input, expected) ->
            assertEquals(expected, input.truncatedHost())
        }
    }

    @Test
    fun `WHEN isYouTubeDesktop called with a null path THEN uri is not YouTube desktop`() {
        val uri = httpsUriBuilder()
            .authority("mozilla.org")
            .path(null)
            .build()
        assertNull(uri.path)

        assertFalse(uri.isYouTubeDesktop)
    }

    @Test
    fun `WHEN isYouTubeDesktop called with a null host THEN uri is not YouTube desktop`() {
        val uri = httpsUriBuilder()
            .authority(null)
            .path("mozilla/en-US")
            .build()
        assertNull(uri.host)

        assertFalse(uri.isYouTubeDesktop)
    }

    @Test
    fun `WHEN isYouTubeDesktop called with non-YT desktop uri THEN uri is not YouTube desktop`() {
        arrayOf(
            "https://mozilla.org",
            "https://m.youtube.com",
            "https://youtube.com/tv"
        ).map { it.toUri()!! }.forEach {
            assertFalse(it.toString(), it.isYouTubeDesktop)
        }
    }

    @Test
    fun `WHEN isYouTubeDesktop called with YT desktop uri THEN uri is YouTube desktop`() {
        arrayOf(
            "https://youtube.com", // home
            "https://youtube.com/", // home with trailing slash
            "https://www.youtube.com/watch?v=IYxZLyW-ANw" // video
        ).map { it.toUri()!! }.forEach {
            assertTrue(it.toString(), it.isYouTubeDesktop)
        }
    }
}

private fun httpsUriBuilder(): Uri.Builder = Uri.Builder().scheme("https")

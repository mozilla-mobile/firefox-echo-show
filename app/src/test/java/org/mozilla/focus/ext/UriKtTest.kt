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

    @Test
    fun testTruncatedPathWithEmptySegments() {
        assertTruncatedPath("", "https://www.mozilla.org")
        assertTruncatedPath("", "https://www.mozilla.org/")
        assertTruncatedPath("", "https://www.mozilla.org///")
    }

    @Test
    fun testTrunactedPathWithOneSegment() {
        assertTruncatedPath("/space", "https://www.theverge.com/space")
    }

    @Test
    fun testTruncatedPathWithTwoSegments() {
        assertTruncatedPath("/en-US/firefox", "https://www.mozilla.org/en-US/firefox/")
        assertTruncatedPath("/mozilla-mobile/focus-android", "https://github.com/mozilla-mobile/focus-android")
    }

    @Test
    fun testTruncatedPathWithMultipleSegments() {
        assertTruncatedPath("/en-US/…/fast", "https://www.mozilla.org/en-US/firefox/features/fast/")

        assertTruncatedPath("/2017/…/nasa-hi-seas-mars-analogue-mission-hawaii-mauna-loa",
            "https://www.theverge.com/2017/9/24/16356876/nasa-hi-seas-mars-analogue-mission-hawaii-mauna-loa")
    }

    @Test
    fun testTruncatedPathWithMultipleSegmentsAndFragment() {
        assertTruncatedPath(
            "/@bfrancis/the-story-of-firefox-os-cb5bf796e8fb",
            "https://medium.com/@bfrancis/the-story-of-firefox-os-cb5bf796e8fb#931a")
    }

    private fun assertTruncatedPath(expectedTruncatedPath: String, url: String) {
        assertEquals("truncatedPath($url)",
            expectedTruncatedPath,
            Uri.parse(url).truncatedPath())
    }
}

private fun httpsUriBuilder(): Uri.Builder = Uri.Builder().scheme("https")

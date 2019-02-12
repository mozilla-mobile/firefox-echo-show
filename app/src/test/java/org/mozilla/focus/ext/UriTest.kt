/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriTest {
    @Test
    fun testTruncatedHostWithCommonUrls() {
        assertTruncatedHost("mozilla.org", "https://www.mozilla.org")
        assertTruncatedHost("wikipedia.org", "https://en.m.wikipedia.org/wiki/")
        assertTruncatedHost("example.org", "http://example.org")
        assertTruncatedHost("youtube.com", "https://www.youtube.com/watch?v=oHg5SJYRHA0")
        assertTruncatedHost("facebook.com", "https://www.facebook.com/Firefox/")
        assertTruncatedHost("yahoo.com", "https://de.search.yahoo.com/search?p=mozilla&fr=yfp-t&fp=1&toggle=1&cop=mss&ei=UTF-8")
        assertTruncatedHost("amazon.co.uk", "https://www.amazon.co.uk/Doctor-Who-10-Part-DVD/dp/B06XCMVY1H")
    }

    private fun assertTruncatedHost(expectedTruncatedPath: String, url: String) {
        assertEquals("truncatedHost($url)",
                expectedTruncatedPath,
                Uri.parse(url).truncatedHost())
    }

    @Test // regression test for #403.
    fun `isYouTubeDesktop is false for null host`() {
        assertFalse(Uri.parse("file:///whatever/").isYouTubeDesktop)
    }

    @Test
    fun `isYouTubeDesktop is true for desktop YouTube URL`() {
        assertTrue(Uri.parse("https://www.youtube.com/watch?v=6E4TOHGO0Ms").isYouTubeDesktop)
    }

    @Test
    fun `isYouTubeDesktop is false for mobile YouTube URL`() {
        assertFalse(Uri.parse("https://m.youtube.com/watch?v=s2txy9sYc9M").isYouTubeDesktop)
    }

    @Test
    fun `isYouTubeDesktop is false for TV YouTube URL`() {
        assertFalse(Uri.parse("https://www.youtube.com/tv#/watch/video/control?v=SsipngvHiEU").isYouTubeDesktop)
    }

    @Test
    fun `isYouTubeDesktop is false for non-YouTube URL`() {
        assertFalse(Uri.parse("https://www.google.com/").isYouTubeDesktop)
    }

    @Test
    fun `isYouTubeDesktop is false for youtube in uri path`() {
        assertFalse(Uri.parse("https://www.google.com/youtube.com").isYouTubeDesktop)
    }
}

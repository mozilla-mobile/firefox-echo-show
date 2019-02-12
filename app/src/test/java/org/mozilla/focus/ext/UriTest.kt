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
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.ext.toUri
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToolbarTextTest {

    @Test
    fun `WHEN getDisplayText receives the home URL THEN it returns a desire for hint text`() {
        assertEquals(HintText, ToolbarText.getDisplayText("firefox:home".toUri() as Uri).rightOrThrow)
    }

    @Test
    fun `WHEN getDisplayText receives an https URI THEN return it`() {
        val expected = "https://www.mozilla.org/en-US/privacy/firefox-fire-tv/"
        assertEquals(expected, ToolbarText.getDisplayText(expected.toUri() as Uri).leftOrThrow)
    }

    @Test
    fun `WHEN getDisplayText receives a file URI that is not an android asset THEN return it`() {
        val expected = "file:///index.html#anchor"
        assertEquals(expected, ToolbarText.getDisplayText(expected.toUri() as Uri).leftOrThrow)
    }

    @Test
    fun `WHEN getDisplayText receives an android asset URI THEN return the app scheme URI`() {
        val inputUri = "file:///android_asset/licenses.html#sentry".toUri() as Uri
        assertEquals("firefox:licenses#sentry", ToolbarText.getDisplayText(inputUri).leftOrThrow)
    }

    @Test
    fun `WHEN getDisplayText receives a uri without a scheme THEN return it`() {
        // Perhaps not the best behavior but this is unlikely to come up in the wild.
        val expected = "there/is/no/spoon"
        assertEquals(expected, ToolbarText.getDisplayText(expected.toUri() as Uri).leftOrThrow)
    }
}

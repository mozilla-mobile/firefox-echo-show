/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.content.res.Configuration
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.StringContains
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BundledTilesManagerTest {

    private lateinit var testManager: BundledTilesManager

    @Before
    fun setup() {
        testManager = BundledTilesManager(linkedMapOf())
    }

    @Test
    fun `GIVEN screen is xlarge WHEN we get the image path in assets THEN returned path includes xlarge`() {
        val mockConfig = mock(Configuration::class.java).apply {
            screenLayout = Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        val actualImagePath = testManager.getImagePathInAssets(mockConfig, "google.png")
        assertThat(actualImagePath, StringContains.containsString("-xlarge"))
    }

    @Test
    fun `GIVEN screen is large WHEN we get the image path in assets THEN returned path does not include qualifier`() {
        val mockConfig = mock(Configuration::class.java).apply {
            screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE
        }

        val actualImagePath = testManager.getImagePathInAssets(mockConfig, "google.png")
        listOf("-xlarge", "-large", "-normal", "-small").forEach {
            assertThat(actualImagePath, not(StringContains.containsString(it)))
        }
    }
}

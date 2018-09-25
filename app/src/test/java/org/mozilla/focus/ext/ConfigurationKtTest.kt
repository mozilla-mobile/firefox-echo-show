/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.res.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import android.content.res.Configuration as Config

class ConfigurationKtTest {

    @Test
    fun `WHEN screenLayout has xlarge bit THEN return isScreenXLarge == true`() {
        val testConfig = mock(Configuration::class.java).apply {
            screenLayout = Config.SCREENLAYOUT_SIZE_XLARGE + Config.SCREENLAYOUT_ROUND_YES
        }
        assertTrue(testConfig.isScreenXLarge)
    }

    @Test
    fun `WHEN screenLayout does not have xlarge bit THEN return isScreenXLarge == false`() {
        val testConfig = mock(Configuration::class.java).apply {
            screenLayout = Config.SCREENLAYOUT_SIZE_UNDEFINED + Config.SCREENLAYOUT_LAYOUTDIR_RTL
        }
        assertFalse(testConfig.isScreenXLarge)
    }
}

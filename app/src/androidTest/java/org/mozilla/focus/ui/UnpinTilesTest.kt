/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.focus.helpers.MainActivityTestRule
import org.mozilla.focus.ui.robots.navigationOverlay

/**
 * This test verifies the unpin tile behavior.
 */
class UnpinTilesTest {

    @JvmField @Rule
    val activityTestRule = MainActivityTestRule()

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun unpinTilesTest() {
        navigationOverlay {
            assertHomeTileCount(3)

        }.longPressTileToUnpinOverlay(2) {
        }.unpinTileToNavigationOverlay {
            assertHomeTileCount(2)

        }.longPressTileIsNoOp(0) {
            // Long pressing the google search tile is a no-op so it cannot be removed. WE RELY ON THIS BEHAVIOR:
            // the navigation overlay does not visually handle the case when all of the home tiles are removed.
            // If this behavior changes, this test should fail and you should also fix the navigation overlay.
            assertHomeTileCount(2)
        }
    }
}

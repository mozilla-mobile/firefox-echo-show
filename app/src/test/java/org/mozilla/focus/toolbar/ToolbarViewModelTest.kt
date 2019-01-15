/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.helpers.ext.assertValues
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToolbarViewModelTest {

    private lateinit var viewModel: ToolbarViewModel

    @Before
    fun setUp() {
        viewModel = ToolbarViewModel()
    }

    @Test
    fun `GIVEN navigation overlay is not visible THEN toolbar is focusable`() {
        viewModel.isToolbarImportantForAccessibility.assertValues(true, true) {
            viewModel.onNavigationOverlayVisibilityChange(false, false)
            viewModel.onNavigationOverlayVisibilityChange(false, true)
        }
    }

    @Test
    fun `GIVEN navigation overlay is visible WHEN overlay is startup overlay is visible THEN toolbar is focusable`() {
        viewModel.isToolbarImportantForAccessibility.assertValues(true) {
            viewModel.onNavigationOverlayVisibilityChange(true, true)
        }
    }

    @Test
    fun `GIVEN navigation overlay is visible WHEN overlay is not startup overlay is visible THEN toolbar is not focusable`() {
        viewModel.isToolbarImportantForAccessibility.assertValues(false) {
            viewModel.onNavigationOverlayVisibilityChange(true, false)
        }
    }
}

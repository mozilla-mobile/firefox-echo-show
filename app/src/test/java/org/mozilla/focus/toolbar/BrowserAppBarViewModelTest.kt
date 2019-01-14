/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.helpers.ext.assertValues
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserAppBarViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var viewModel: BrowserAppBarViewModel

    @Before
    fun setUp() {
        viewModel = BrowserAppBarViewModel()
    }

    @Test
    fun `GIVEN voiceView is disabled WHEN navigation overlay is visible THEN toolbar scroll is disabled`() {
        viewModel.setIsVoiceViewEnabled(false)
        viewModel.setIsNavigationOverlayVisible(true)
        viewModel.isToolbarScrollEnabled.assertValues(false) {}
    }

    @Test
    fun `GIVEN voiceView is disabled WHEN navigation overlay is not visible THEN toolbar scroll is enabled`() {
        viewModel.setIsVoiceViewEnabled(false)
        viewModel.setIsNavigationOverlayVisible(false)
        viewModel.isToolbarScrollEnabled.assertValues(true) {}
    }

    @Test
    fun `GIVEN voiceView is enabled THEN toolbar scroll is always disabled`() {
        viewModel.setIsVoiceViewEnabled(true)
        viewModel.setIsNavigationOverlayVisible(false)
        viewModel.isToolbarScrollEnabled.assertValues(false, false) {
            viewModel.setIsNavigationOverlayVisible(true)
        }
    }
}

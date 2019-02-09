/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.focus.architecture.FrameworkRepo
import org.mozilla.focus.helpers.ext.assertValues
import org.mozilla.focus.session.SessionRepo
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserAppBarViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var viewModel: BrowserAppBarViewModel

    private lateinit var frameworkRepo: FrameworkRepo
    private lateinit var isVoiceViewEnabled: MutableLiveData<Boolean>

    private lateinit var sessionRepo: SessionRepo
    private lateinit var isFullscreen: MutableLiveData<Boolean>

    @Before
    fun setUp() {
        isVoiceViewEnabled = MutableLiveData()
        frameworkRepo = mock(FrameworkRepo::class.java).also {
            `when`(it.isVoiceViewEnabled).thenReturn(isVoiceViewEnabled)
        }

        isFullscreen = MutableLiveData()
        sessionRepo = mock(SessionRepo::class.java).also {
            `when`(it.isFullscreen).thenReturn(isFullscreen)
        }

        viewModel = BrowserAppBarViewModel(frameworkRepo, sessionRepo)
    }

    @Test
    fun `GIVEN voiceView is disabled WHEN navigation overlay is visible THEN toolbar scroll is disabled`() {
        viewModel.isToolbarScrollEnabled.assertValues(false) {
            isVoiceViewEnabled.value = false
            viewModel.onNavigationOverlayVisibilityChange(true)
        }
    }

    @Test
    fun `GIVEN voiceView is disabled WHEN navigation overlay is not visible THEN toolbar scroll is enabled`() {
        viewModel.isToolbarScrollEnabled.assertValues(true) {
            isVoiceViewEnabled.value = false
            viewModel.onNavigationOverlayVisibilityChange(false)
        }
    }

    @Test
    fun `GIVEN voiceView is enabled THEN toolbar scroll is always disabled`() {
        viewModel.isToolbarScrollEnabled.assertValues(false, false) {
            isVoiceViewEnabled.value = true
            viewModel.onNavigationOverlayVisibilityChange(false)
            viewModel.onNavigationOverlayVisibilityChange(true)
        }
    }

    @Test
    fun `WHEN session repo isFullscreen is changed THEN the app bar takes the opposite value`() {
        viewModel.isAppBarHidden.assertValues(false, true, false) {
            isFullscreen.value = true
            isFullscreen.value = false
            isFullscreen.value = true
        }
    }
}

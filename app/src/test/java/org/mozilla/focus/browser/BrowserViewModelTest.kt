/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.focus.architecture.FrameworkRepo
import org.mozilla.focus.helpers.ext.assertValues
import org.mozilla.focus.session.SessionRepo
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var viewModel: BrowserViewModel

    private lateinit var frameworkRepo: FrameworkRepo
    private lateinit var isVoiceViewEnabled: MutableLiveData<Boolean>

    private lateinit var sessionRepo: SessionRepo

    @Before
    fun setUp() {
        isVoiceViewEnabled = MutableLiveData()
        frameworkRepo = mock(FrameworkRepo::class.java).also {
            `when`(it.isVoiceViewEnabled).thenReturn(isVoiceViewEnabled)
        }

        sessionRepo = mock(SessionRepo::class.java)

        viewModel = BrowserViewModel(frameworkRepo, sessionRepo)
    }

    @Test
    fun `GIVEN VoiceView is disabled THEN web view is visible`() {
        viewModel.isWebViewVisible.assertValues(true, true) {
            isVoiceViewEnabled.value = false
            viewModel.onNavigationOverlayVisibilityChange(true)
            viewModel.onNavigationOverlayVisibilityChange(false)
        }
    }

    @Test
    fun `GIVEN VoiceView is enabled WHEN nav overlay is not visible THEN web view is visible`() {
        viewModel.isWebViewVisible.assertValues(true) {
            isVoiceViewEnabled.value = true
            viewModel.onNavigationOverlayVisibilityChange(false)
        }
    }

    @Test
    fun `GIVEN VoiceView is enabled WHEN nav overlay is visible THEN web view is not visible`() {
        viewModel.isWebViewVisible.assertValues(false) {
            isVoiceViewEnabled.value = true
            viewModel.onNavigationOverlayVisibilityChange(true)
        }
    }

    @Test
    fun `WHEN fullscreenChanged is called THEN sessionRepo is notified`() {
        viewModel.fullscreenChanged(false)
        verify(sessionRepo, times(1)).fullscreenChange(false)
        viewModel.fullscreenChanged(true)
        verify(sessionRepo, times(1)).fullscreenChange(true)
    }
}

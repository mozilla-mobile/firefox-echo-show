/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineContext
import kotlinx.coroutines.test.withTestContext
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
import java.util.concurrent.TimeUnit

private const val EXIT_FULLSCREEN_EXCEED_EMISSION_DELAY_SECONDS = 6L

@ObsoleteCoroutinesApi // TestCoroutineContext: it has no replacement yet. See https://stackoverflow.com/a/49078296 for an alternative.
@RunWith(RobolectricTestRunner::class)
class BrowserViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var viewModel: BrowserViewModel

    private lateinit var frameworkRepo: FrameworkRepo
    private lateinit var isVoiceViewEnabled: MutableLiveData<Boolean>

    private lateinit var sessionRepo: SessionRepo
    private lateinit var isFullscreen: MutableLiveData<Boolean>

    private lateinit var testCoroutineContext: TestCoroutineContext

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

        testCoroutineContext = TestCoroutineContext()
        viewModel = BrowserViewModel(frameworkRepo, sessionRepo, testCoroutineContext)
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

    @Test
    fun `WHEN entering fullscreen THEN the window background is enabled immediately`() {
        viewModel.isWindowBackgroundEnabled.assertValues(false, true) {
            isFullscreen.value = false
            isFullscreen.value = true
        }
    }

    @Test
    fun `WHEN exiting fullscreen THEN the window background is not disabled until several seconds afterwards`() {
        withTestContext(testCoroutineContext) {
            viewModel.isWindowBackgroundEnabled.assertValues(true) {
                isFullscreen.value = true
                isFullscreen.value = false // note: emission is delayed so no assertion.
            }

            viewModel.isWindowBackgroundEnabled.assertValues(true, false) {
                advanceTimeBy(EXIT_FULLSCREEN_EXCEED_EMISSION_DELAY_SECONDS, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `WHEN sessionRepo isFullscreen emits redundant values THEN the window background state still updates correctly`() {
        withTestContext(testCoroutineContext) {
            viewModel.isWindowBackgroundEnabled.assertValues(true, true, true, false, false, false, false, true, true, true) {
                repeat(3) { isFullscreen.value = true }
                repeat(3) { isFullscreen.value = false }
                advanceTimeBy(EXIT_FULLSCREEN_EXCEED_EMISSION_DELAY_SECONDS, TimeUnit.SECONDS)
                repeat(3) { isFullscreen.value = false }
                repeat(3) { isFullscreen.value = true }
            }
        }
    }

    @Test
    fun `WHEN entering, exiting, and entering fullscreen quickly THEN the window background defer disable is cancelled`() {
        withTestContext(testCoroutineContext) {
            viewModel.isWindowBackgroundEnabled.assertValues(true) {
                isFullscreen.value = true
                isFullscreen.value = false // note: emission is delayed so no assertion.
            }

            // We can verify cancellation occurs if the window background is not disabled (false is not emitted).
            viewModel.isWindowBackgroundEnabled.assertValues(true, true) {
                advanceTimeBy(EXIT_FULLSCREEN_EXCEED_EMISSION_DELAY_SECONDS / 2, TimeUnit.SECONDS)
                isFullscreen.value = true
                advanceTimeBy(EXIT_FULLSCREEN_EXCEED_EMISSION_DELAY_SECONDS, TimeUnit.SECONDS)
            }

            // Verify windowBackground disabled deferred updates still works after a successful cancellation.
            viewModel.isWindowBackgroundEnabled.assertValues(true, false) {
                isFullscreen.value = false
                advanceTimeBy(EXIT_FULLSCREEN_EXCEED_EMISSION_DELAY_SECONDS, TimeUnit.SECONDS)
            }
        }
    }
}

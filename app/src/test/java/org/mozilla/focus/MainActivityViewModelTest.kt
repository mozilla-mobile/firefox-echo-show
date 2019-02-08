/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.focus.helpers.ext.assertValues
import org.mozilla.focus.session.SessionRepo

class MainActivityViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: MainActivityViewModel

    private lateinit var sessionRepo: SessionRepo
    private lateinit var isFullscreen: MutableLiveData<Boolean>

    @Before
    fun setUp() {
        isFullscreen = MutableLiveData()
        sessionRepo = mock(SessionRepo::class.java).also {
            `when`(it.isFullscreen).thenReturn(isFullscreen)
        }
        viewModel = MainActivityViewModel(sessionRepo)
    }

    @Test
    fun `WHEN sessionRepo isFullscreen is changed THEN isWindowBackground enabled represents that value`() {
        viewModel.isWindowBackgroundEnabled.assertValues(false, true, false) {
            isFullscreen.value = false
            isFullscreen.value = true
            isFullscreen.value = false
        }
    }
}

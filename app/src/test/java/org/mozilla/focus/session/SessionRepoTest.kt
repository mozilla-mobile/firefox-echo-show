/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.focus.helpers.ext.assertValues

private const val IS_FULLSCREEN_INITIAL_VALUE = false

class SessionRepoTest {

    @get:Rule val rule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var repo: SessionRepo

    @Before
    fun setUp() {
        repo = SessionRepo()
    }

    @Test
    fun `GIVEN the SessionRepo is created THEN isFullscreen is false`() {
        assertFalse(repo.isFullscreen.value!!)
    }

    @Test
    fun `WHEN fullscreenChange is called THEN isFullscreen reflects that value`() {
        repo.isFullscreen.assertValues(IS_FULLSCREEN_INITIAL_VALUE, false, true, false) {
            repo.fullscreenChange(false)
            repo.fullscreenChange(true)
            repo.fullscreenChange(false)
        }
    }
}

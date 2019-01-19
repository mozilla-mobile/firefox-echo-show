/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.arch.lifecycle.Lifecycle
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class ActivityUiCoroutineScopeTest {

    private lateinit var uninitScope: ActivityUiCoroutineScope
    private lateinit var initScope: ActivityUiCoroutineScope

    @Before
    fun setUp() {
        uninitScope = ActivityUiCoroutineScope()
        initScope = ActivityUiCoroutineScope().apply {
            init(mock(Lifecycle::class.java))
        }
    }

    @Test
    fun `WHEN init is called THEN the lifecycle observer is observing the lifecycle`() {
        val lifecycle = mock(Lifecycle::class.java)
        uninitScope.init(lifecycle)

        verify(lifecycle, times(1)).addObserver(any())
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN the coroutine context is accessed before init THEN an exception is thrown`() {
        uninitScope.coroutineContext
    }

    @Test
    fun `WHEN onDestroy is not called THEN the coroutine context is not cancelled`() = runBlocking {
        assertTrue(initScope.coroutineContext.isActive)
    }

    @Test
    fun `WHEN onDestroy is called THEN the coroutine context is cancelled`() = runBlocking {
        initScope.onDestroy()
        assertFalse(initScope.coroutineContext.isActive)
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import kotlinx.coroutines.isActive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.IllegalStateException

class FragmentViewUiCoroutineScopeTest {

    private lateinit var scope: FragmentViewUiCoroutineScope

    @Before
    fun setUp() {
        scope = FragmentViewUiCoroutineScope()
    }

    @Test(expected = KotlinNullPointerException::class)
    fun `WHEN coroutineContext is accessed before onCreateView THEN an exception is thrown`() {
        scope.coroutineContext
    }

    @Test
    fun `WHEN onCreateView is called and onDestroyView is called THEN accessing the scope will not throw an exception`() {
        scope.onCreateView()
        scope.onDestroyView()
        scope.coroutineContext
    }

    @Test(expected = KotlinNullPointerException::class)
    fun `WHEN onDestroyView is called before onCreateView THEN an exception is thrown`() {
        scope.onDestroyView()
    }

    @Test
    fun `WHEN onCreateView is called but onDestroyView is not THEN the scope is not cancelled`() {
        scope.onCreateView()
        assertTrue(scope.coroutineContext.isActive)
    }

    @Test
    fun `WHEN onCreateView is called and onDestroyView is called THEN the scope is cancelled`() {
        scope.onCreateView()
        scope.onDestroyView()
        assertFalse(scope.coroutineContext.isActive)
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN onCreateView is called twice before onDestroyView is called THEN then an exception is thrown`() {
        scope.onCreateView()
        scope.onCreateView()
    }
}

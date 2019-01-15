/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class LiveDataDoOnEachTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var observerSpy: Observer<Int>
    private lateinit var liveData: MutableLiveData<Int>
    private var uninitializedValue: Int? = null

    @Before
    fun setup() {
        liveData = MutableLiveData()
    }

    @Test
    fun `side effects should be executed`() {
        observerSpy = spy(Observer { })

        liveData.doOnEach { uninitializedValue = it }
            .observeForever(observerSpy)

        liveData.value = 1
        assertNotNull(uninitializedValue)
        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `passed value should not be changed`() {
        observerSpy = spy(Observer { assertEquals(1, it) })

        liveData.doOnEach { uninitializedValue = it!! * 5 }
            .observeForever(observerSpy)

        liveData.value = 1
        verify(observerSpy, times(1)).onChanged(any())
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.focus.architecture.NonNullMutableLiveData
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SessionRestorerTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var restorer: SessionRestorer

    private lateinit var storage: SessionRestorerStorage

    private lateinit var currentUrl: NonNullMutableLiveData<String>
    private lateinit var currentSession: Session
    private lateinit var sessionManager: SessionManager

    private var currentTimeMillis: Long = -1

    @Before
    fun setUp() {
        storage = mock(SessionRestorerStorage::class.java)

        currentSession = mock(Session::class.java).also {
            currentUrl = NonNullMutableLiveData("")
            `when`(it.url).thenReturn(currentUrl)
        }
        sessionManager = mock(SessionManager::class.java).also {
            `when`(it.currentSession).thenReturn(currentSession)
        }

        currentTimeMillis = 0

        restorer = SessionRestorer(
            storage,
            sessionManager,
            { currentTimeMillis }
        )
    }

    @Test
    fun `WHEN getAndInit is called THEN a lifecycle observer is added`() {
        val lifecycle = mock(Lifecycle::class.java)
        SessionRestorer.getAndInit(
            lifecycle,
            storage,
            sessionManager
        )

        verify(lifecycle, times(1)).addObserver(any())
    }

    @Test
    fun `WHEN onPause is called THEN the session will be persisted`() {
        val expected = PersistableSession("https://mozilla.org", 1776).also {
            currentUrl.value = it.url
            currentTimeMillis = it.timeMillis
        }

        restorer.onPause()

        verify(storage, times(1)).setPersistedSession(expected)
    }

    @Test
    fun `GIVEN no persisted session WHEN getPersistedSessionUrl is called THEN null is returned`() {
        `when`(storage.getPersistedSession()).thenReturn(null)
        assertNull(restorer.getPersistedSessionUrl())
    }

    @Test
    fun `GIVEN a persisted session greater than five minutes ago WHEN getPersistedSession is called THEN null is returned`() {
        currentTimeMillis = 1000000
        `when`(storage.getPersistedSession()).thenReturn(PersistableSession(
            "https://mozilla.org",
            currentTimeMillis - TimeUnit.MINUTES.toMillis(6)
        ))

        assertNull(restorer.getPersistedSessionUrl())
    }

    @Test
    fun `GIVEN a persisted session less than five minutes ago WHEN getPersistedSession is called THEN the session is returned`() {
        currentTimeMillis = 1000000
        val expected = PersistableSession(
            "https://mozilla.org",
            currentTimeMillis - TimeUnit.MINUTES.toMillis(4)
        )
        `when`(storage.getPersistedSession()).thenReturn(expected)

        assertEquals(expected.url, restorer.getPersistedSessionUrl())
    }

    @Test
    fun `WHEN clearPersistedSession is called THEN storage is requested to clear itself`() {
        restorer.clearData()
        verify(storage, times(1)).clearPersistedSession()
    }
}

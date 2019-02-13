/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.session.SessionRestorerStorage.Companion.KEY_TIME_MILLIS
import org.mozilla.focus.session.SessionRestorerStorage.Companion.KEY_URL
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val URL_EXAMPLE = "https://mozilla.org"

@RunWith(RobolectricTestRunner::class)
class SessionRestorerStorageTest {

    private lateinit var storage: SessionRestorerStorage
    private lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setUp() {
        sharedPrefs = RuntimeEnvironment.application.getSharedPreferences("SessionRestorerStorageTest", 0)
        storage = SessionRestorerStorage { _, _ -> sharedPrefs }
    }

    @Test
    fun `GIVEN no persisted session WHEN getPersistedSession is called THEN null is returned`() {
        assertNull(storage.getPersistedSession())
    }

    @Test
    fun `GIVEN a mistakenly partially persisted session WHEN getPersistedSession is called THEN null is returned`() {
        sharedPrefs.edit().putString(KEY_URL, URL_EXAMPLE).apply()
        assertNull(storage.getPersistedSession())

        sharedPrefs.edit().clear().apply()
        sharedPrefs.edit().putLong(KEY_TIME_MILLIS, 1000)
        assertNull(storage.getPersistedSession())
    }

    @Test
    fun `GIVEN a persisted session WHEN getPersistedSession is called THEN the session is returned`() {
        val expected = PersistableSession(URL_EXAMPLE, 1776).also {
            it.persistForTest()
        }

        assertEquals(expected, storage.getPersistedSession())
    }

    @Test
    fun `WHEN setPersistedSession is called THEN sharedPrefs is updated with the session values`() {
        val expected = PersistableSession(URL_EXAMPLE, 1776)
        storage.setPersistedSession(expected)

        assertPersistedSession(expected)
    }

    @Test
    fun `GIVEN no persisted session WHEN clearPersistedSession is called THEN sharedPrefs is empty`() {
        storage.clearPersistedSession()
        assertTrue(sharedPrefs.all.isEmpty())
    }

    @Test
    fun `GIVEN a persisted session WHEN clearPersistedSession is called THEN sharedPrefs is empty`() {
        PersistableSession(URL_EXAMPLE, 1600).also {
            it.persistForTest()
        }
        storage.clearPersistedSession()

        assertTrue(sharedPrefs.all.isEmpty())
    }

    private fun assertPersistedSession(expected: PersistableSession): Unit = with(expected) {
        assertEquals(url, sharedPrefs.getString(KEY_URL, "defaultValue"))
        assertEquals(timeMillis, sharedPrefs.getLong(KEY_TIME_MILLIS, -1))
    }

    private fun PersistableSession.persistForTest() {
        sharedPrefs.edit()
            .putString(KEY_URL, url)
            .putLong(KEY_TIME_MILLIS, timeMillis)
            .apply()
    }
}

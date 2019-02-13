/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.content.SharedPreferences
import android.support.annotation.VisibleForTesting
import android.support.annotation.VisibleForTesting.PRIVATE

private const val SHARED_PREFS_NAME = "SessionRestorer"

/**
 * A session as persisted by the [SessionRestorer].
 */
data class PersistableSession(
    val url: String,
    val timeMillis: Long
)

/**
 * The storage layer for [SessionRestorer], decoupling the primary logic from its storage mechanism.
 *
 * @param sharedPrefsProvider A function which returns [SharedPreferences]. Expected: `context::getSharedPreferences`
 */
class SessionRestorerStorage(
    sharedPrefsProvider: (name: String, mode: Int) -> SharedPreferences
) {

    private val sharedPrefs = sharedPrefsProvider(SHARED_PREFS_NAME, 0)

    fun setPersistedSession(persistableSession: PersistableSession): Unit = with(persistableSession) {
        sharedPrefs.edit()
            .putString(KEY_URL, url)
            .putLong(KEY_TIME_MILLIS, timeMillis)
            .apply()
    }

    fun getPersistedSession(): PersistableSession? {
        val url = sharedPrefs.getString(KEY_URL, null)
        val timeMillis = sharedPrefs.getLong(KEY_TIME_MILLIS, -1)

        return if (url == null || timeMillis < 0) {
            null
        } else {
            PersistableSession(url, timeMillis)
        }
    }

    fun clearPersistedSession() {
        sharedPrefs.edit()
            .clear()
            .apply()
    }

    companion object {
        @VisibleForTesting(otherwise = PRIVATE) val KEY_URL = "url"
        @VisibleForTesting(otherwise = PRIVATE) val KEY_TIME_MILLIS = "timeMillis"
    }
}

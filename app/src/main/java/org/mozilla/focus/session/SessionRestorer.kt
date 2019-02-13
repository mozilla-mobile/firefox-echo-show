/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Lifecycle.Event.ON_PAUSE
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.support.annotation.VisibleForTesting
import android.support.annotation.VisibleForTesting.PRIVATE
import java.util.concurrent.TimeUnit

/**
 * An implementation of session restore that persists the last url the user visited before the application
 * was closed: i.e. it does not persist the forward and back stack.
 *
 * To use this, create an instance with [getAndInit] and call [getPersistedSessionUrl] before Activity.onPause
 * to retrieve the last persisted url: persistence happens automatically.
 */
class SessionRestorer @VisibleForTesting(otherwise = PRIVATE) constructor(
    private val storage: SessionRestorerStorage,
    private val sessionManager: SessionManager,
    private val getCurrentTimeMillis: () -> Long = { System.currentTimeMillis() } // for easier mocking.
) : LifecycleObserver {

    private fun init(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    /**
     * @return the URL of the persisted session if that session should be restored, null otherwise.
     */
    fun getPersistedSessionUrl(): String? {
        val persistedSession = storage.getPersistedSession() ?: return null
        val timeMillisOfPersistence = persistedSession.timeMillis
        val timeMinutesSincePersistence =
            TimeUnit.MILLISECONDS.toMinutes(getCurrentTimeMillis() - timeMillisOfPersistence)

        val shouldRestoreSession = timeMinutesSincePersistence < 5
        return if (shouldRestoreSession) persistedSession.url else null
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun onPause() {
        // We must persist the session when the application is closing. However, unlike stock Android, onStop
        // will *not* be called if the application is open and times out. Instead, we must persist here.
        storage.setPersistedSession(PersistableSession(
            url = sessionManager.currentSession.url.value,
            timeMillis = getCurrentTimeMillis()
        ))
    }

    fun clearData() {
        storage.clearPersistedSession()
    }

    companion object {
        fun getAndInit(
            lifecycle: Lifecycle,
            storage: SessionRestorerStorage,
            sessionManager: SessionManager
        ): SessionRestorer {
            return SessionRestorer(storage, sessionManager).apply {
                init(lifecycle)
            }
        }
    }
}

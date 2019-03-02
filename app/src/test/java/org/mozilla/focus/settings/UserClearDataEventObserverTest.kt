/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.support.v7.app.AppCompatActivity
import mozilla.components.support.base.observer.Consumable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.focus.session.SessionRestorer
import org.mozilla.focus.utils.LiveDataEvent
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserClearDataEventObserverTest {

    private lateinit var observer: UserClearDataEventObserver
    private lateinit var sessionRestorer: SessionRestorer

    @Before
    fun setUp() {
        sessionRestorer = mock(SessionRestorer::class.java)

        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().get()
        observer = UserClearDataEventObserver(activity, sessionRestorer)
    }

    @Test
    fun `WHEN a null event is received THEN the session restorer is not cleared`() {
        observer.onChanged(null)
        verify(sessionRestorer, never()).clearData()
    }

    @Test
    fun `WHEN a clear data event is received THEN the session restorer should be cleared`() {
        observer.onChanged(Consumable.from(LiveDataEvent))
        verify(sessionRestorer, times(1)).clearData()
    }
}

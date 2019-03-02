/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
import android.os.Bundle
import mozilla.components.support.utils.SafeIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.focus.ext.toSafeIntent
import org.mozilla.focus.ext.toUri
import org.mozilla.focus.session.SessionRestorer
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val TEST_URL_INTENT = "https://github.com/mozilla-mobile/focus-android"
private const val TEST_URL_SESSION_RESTORER = "https://mozilla.org"

@RunWith(RobolectricTestRunner::class)
class MainActivityIntentResponderTest {

    private val context get() = RuntimeEnvironment.application

    private lateinit var responder: MainActivityIntentResponder
    private lateinit var loadUrlFun: (String) -> Unit

    private lateinit var sessionRestorerWithSession: SessionRestorer
    private lateinit var sessionRestorerNoSession: SessionRestorer
    private lateinit var viewIntent: SafeIntent
    private lateinit var invalidViewIntent: SafeIntent

    @Before
    fun setUp() {
        loadUrlFun = {}
        responder = MainActivityIntentResponder { loadUrlFun(it) }

        sessionRestorerWithSession = mock(SessionRestorer::class.java).also {
            `when`(it.getPersistedSessionUrl()).thenReturn(TEST_URL_SESSION_RESTORER)
        }
        sessionRestorerNoSession = mock(SessionRestorer::class.java).also {
            `when`(it.getPersistedSessionUrl()).thenReturn(null)
        }

        viewIntent = Intent(ACTION_VIEW, TEST_URL_INTENT.toUri()).toSafeIntent()
        invalidViewIntent = Intent(ACTION_VIEW).toSafeIntent()
    }

    @Test
    fun `WHEN receiving a view intent in onCreate with the launched from history flag THEN a url is not loaded`() {
        val intent = viewIntent.unsafe.apply {
            addFlags(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        }.toSafeIntent()

        setLoadUrlIsFail()
        responder.onCreate(context, null, sessionRestorerNoSession, intent)
    }

    @Test
    fun `WHEN the activity is restoring state THEN no url is loaded`() {
        setLoadUrlIsFail()
        responder.onCreate(context, mock(Bundle::class.java), sessionRestorerNoSession, viewIntent)
    }

    @Test
    fun `GIVEN there is no session to restore WHEN receiving an invalid view intent in onCreate THEN no url is loaded`() {
        setLoadUrlIsFail()
        responder.onCreate(context, null, sessionRestorerNoSession, invalidViewIntent)
    }

    @Test
    fun `WHEN receiving a valid view intent in onCreate THEN a url is loaded`() {
        assertLoadUrlIsCalled {
            responder.onCreate(context, null, sessionRestorerNoSession, viewIntent)
        }
    }

    @Test
    fun `GIVEN there is a session to restore WHEN receiving a valid view intent THEN the intent uri is loaded`() {
        assertLoadUrlIsCalled(TEST_URL_INTENT) {
            responder.onCreate(context, null, sessionRestorerWithSession, viewIntent)
        }
    }

    @Test
    fun `GIVEN there is a session to restore WHEN receiving a valid main intent THEN the restored session uri is loaded`() {
        val mainIntent = Intent(ACTION_MAIN).toSafeIntent()
        assertLoadUrlIsCalled(TEST_URL_SESSION_RESTORER) {
            responder.onCreate(context, null, sessionRestorerWithSession, mainIntent)
        }
    }

    @Test
    fun `WHEN receiving an invalid view intent in onNewIntent THEN no url is loaded`() {
        setLoadUrlIsFail()
        responder.onNewIntent(context, invalidViewIntent)
    }

    @Test
    fun `WHEN receiving a valid view intent in onNewIntent THEN a url is loaded`() {
        assertLoadUrlIsCalled {
            responder.onNewIntent(context, viewIntent)
        }
    }

    private fun assertLoadUrlIsCalled(withString: String? = null, testFunction: () -> Unit) {
        var isCalled = false
        loadUrlFun = { actual ->
            isCalled = true
            withString?.let { expected -> assertEquals("Loaded URL did not match expected", expected, actual) }
        }
        testFunction()
        assertTrue("Expected load url to be called", isCalled)
    }

    private fun setLoadUrlIsFail() {
        loadUrlFun = { fail("Did not expect loadUrl to get called") }
    }
}

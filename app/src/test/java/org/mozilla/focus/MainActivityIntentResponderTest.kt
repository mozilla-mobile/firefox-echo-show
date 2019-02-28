/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
import android.os.Bundle
import mozilla.components.support.utils.SafeIntent
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mozilla.focus.ext.toSafeIntent
import org.mozilla.focus.ext.toUri
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val TEST_URL = "https://github.com/mozilla-mobile/focus-android"

@RunWith(RobolectricTestRunner::class)
class MainActivityIntentResponderTest {

    private val context get() = RuntimeEnvironment.application

    private lateinit var responder: MainActivityIntentResponder
    private lateinit var loadUrlFun: (String) -> Unit

    private lateinit var intent: SafeIntent

    @Before
    fun setUp() {
        loadUrlFun = {}
        responder = MainActivityIntentResponder { loadUrlFun(it) }

        intent = Intent(ACTION_VIEW, TEST_URL.toUri()).toSafeIntent()
    }

    @Test
    fun `WHEN receiving a view intent in onCreate with the launched from history flag THEN a url is not loaded`() {
        val intent = intent.unsafe.apply {
            addFlags(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        }.toSafeIntent()

        setLoadUrlIsFail()
        responder.onCreate(context, null, intent)
    }

    @Test
    fun `WHEN the activity is restoring state THEN no url is loaded`() {
        setLoadUrlIsFail()
        responder.onCreate(context, mock(Bundle::class.java), intent)
    }

    private fun setLoadUrlIsFail() {
        loadUrlFun = { fail("Did not expect loadUrl to get called") }
    }
}

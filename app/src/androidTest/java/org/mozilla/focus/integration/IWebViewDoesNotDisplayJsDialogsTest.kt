/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.integration

import android.support.test.espresso.IdlingRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.MainActivity
import org.mozilla.focus.ext.getBrowserFragment
import org.mozilla.focus.helpers.DOMAssert.assertBodyText
import org.mozilla.focus.helpers.SessionLoadedIdlingResource
import org.mozilla.focus.helpers.ToolbarInteractor

/**
 * A test to ensure JsDialogs are disabled. This test is especially important to ensure this is
 * working if we replace WebView with GeckoView.
 *
 * Consult with product before removing this functionality.
 */
@RunWith(AndroidJUnit4::class)
class IWebViewDoesNotDisplayJsDialogsTest {

    // Set-up/tear-down code copied from IWebViewExecuteJavascriptTest to save time: we should try
    // to deduplicate.
    @Rule
    @JvmField
    val activityTestRule = object : ActivityTestRule<MainActivity>(MainActivity::class.java) {
        override fun beforeActivityLaunched() { super.beforeActivityLaunched() }
    }

    private lateinit var loadingIdlingResource: SessionLoadedIdlingResource
    private lateinit var mockServer: MockWebServer

    @Before
    fun setUp() {
        loadingIdlingResource = SessionLoadedIdlingResource()
        IdlingRegistry.getInstance().register(loadingIdlingResource)

        mockServer = MockWebServer()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(loadingIdlingResource)
        activityTestRule.getActivity().finishAndRemoveTask()
    }

    @Test
    fun iWebViewDoesNotDisplayJsDialogsTest() {
        val initialBodyText = IWebViewDoesNotDisplayJsDialogsTest::class.java.simpleName
        mockServer.enqueue(MockResponse().setBody("<html><body>$initialBodyText</body></html>"))
        mockServer.start()
        val url = mockServer.url("").toString()

        // Load page
        ToolbarInteractor.enterAndSubmitURL(url)

        // Wait for page load.
        assertBodyText(initialBodyText)

        // Trigger dialogs.
        val expectedBodyText = "end body text"
        val webView = activityTestRule.activity?.supportFragmentManager?.getBrowserFragment()?.webView!!
        activityTestRule.runOnUiThread {
            // Each dialog is a blocking call until the user interacts so the body content
            // mutation should not execute since we do not interact with the dialog.
            webView.evalJS("""
                alert('alert dialog');
                confirm('confirm dialog');
                prompt('prompt dialog');

                document.getElementsByTagName('body')[0].innerText = '$expectedBodyText';
            """.trimIndent())
        }

        // Assert: on failure, we expect this test to time out because the alert calls are blocking.
        //
        // A more intuitive way might be to run a JS dialog command (like alert) and assert if a
        // dialog appears. However, we deliberately avoid this because the implementation can change
        // depending on who's doing it: e.g. the dialogs can be created in web content or native code,
        // they may not contain the content they're supposed to, etc. We chose the current
        // implementation because both desktop Chrome and Firefox are consistent that alert will
        // block until the user interacts with the dialog.
        //
        // We need to include some code for Java after evalJS to prevent the test from ending.
        assertBodyText(expectedBodyText)
    }
}

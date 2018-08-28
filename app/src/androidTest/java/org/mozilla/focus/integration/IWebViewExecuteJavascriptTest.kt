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
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.helpers.DOMAssert.assertBodyText
import org.mozilla.focus.helpers.SessionLoadedIdlingResource
import org.mozilla.focus.helpers.ToolbarInteractor

/** An integration test to verify [IWebView.executeJS] works correctly. */
@RunWith(AndroidJUnit4::class)
class IWebViewExecuteJavascriptTest {

    @Rule @JvmField
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
    fun executeJSTest() {
        val expectedLoadedText = "Loaded"
        mockServer.enqueue(MockResponse().setBody("<html><body>$expectedLoadedText</body></html>"))
        mockServer.start()
        val url = mockServer.url("").toString()

        // Load the mock page.
        ToolbarInteractor.enterAndSubmitURL(url)

        // Assert loaded.
        assertBodyText(expectedLoadedText)

        // Inject JS.
        val expectedChangedText = "Changed"
        val browserFragment = activityTestRule.activity.supportFragmentManager.findFragmentByTag(
                BrowserFragment.FRAGMENT_TAG) as BrowserFragment
        val webView = browserFragment.webView!!
        activityTestRule.runOnUiThread {
            webView.evalJS(
                    "document.getElementsByTagName('body')[0].innerText = '$expectedChangedText';")
        }

        // Assert JS was injected.
        assertBodyText(expectedChangedText)
    }
}

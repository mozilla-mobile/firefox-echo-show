/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webview

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mozilla.focus.iwebview.IWebView
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@Suppress("DEPRECATION") // we use deprecated methods in the client
@RunWith(RobolectricTestRunner::class) // relies on android.net.Uri
class FocusWebViewClientTest {

    private lateinit var context: Context
    private lateinit var testClient: FocusWebViewClient

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        testClient = FocusWebViewClient(context).apply {
            setCallback(mock(IWebView.Callback::class.java)) // a lot of code assumes there is a callback.
        }
    }

    @Test // regression test: #500
    fun `WHEN shouldOverrideUrlLoading is passed an android intent uri THEN it overrides loading and doesn't load a URL`() {
        val webView = mock(FirefoxWebView::class.java).also {
            `when`(it.context).thenReturn(context)
        }

        // This uri is used when clicking on the Messenger icon on m.facebook.com.
        val androidIntentUri = "intent://threads/?vcuid=100000151847025&src=mtouch_diode&show_multiaccount=true#Intent;scheme=fb-messenger;package=com.facebook.orca;end"
        assertTrue(testClient.shouldOverrideUrlLoading(webView, androidIntentUri))
        verify(webView, never()).loadUrl(anyString())
    }
}

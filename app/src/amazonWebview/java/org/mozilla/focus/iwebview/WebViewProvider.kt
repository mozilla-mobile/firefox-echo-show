/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.iwebview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewDatabase
import org.mozilla.focus.R
import org.mozilla.focus.browser.UserAgent
import org.mozilla.focus.webview.FirefoxWebChromeClient
import org.mozilla.focus.webview.FirefoxWebView
import org.mozilla.focus.webview.FocusWebViewClient
import org.mozilla.focus.webview.TrackingProtectionWebViewClient
import org.mozilla.telemetry.BuildConfig

/** Creates a WebView-based IWebView implementation. */
object WebViewProvider {
    /**
     * Preload webview data. This allows the webview implementation to load resources and other data
     * it might need, in advance of intialising the view (at which time we are probably wanting to
     * show a website immediately).
     */
    @JvmStatic
    fun preload(context: Context) {
        TrackingProtectionWebViewClient.triggerPreload(context)
    }

    @JvmStatic
    fun create(context: Context, attrs: AttributeSet): View {
        val client = FocusWebViewClient(context.applicationContext)
        val chromeClient = FirefoxWebChromeClient()

        return FirefoxWebView(context, attrs, client, chromeClient).apply {
            setWebViewClient(client)
            setWebChromeClient(chromeClient)

            initWebview(this)
            initWebSettings(context, settings)
        }
    }

    @Suppress("DEPRECATION") // To be safe, we'll use delete methods as long as they're there.
    fun deleteGlobalData(context: Context) {
        // We don't care about the callback - we just want to make sure cookies are gone
        CookieManager.getInstance().removeAllCookies(null)

        WebStorage.getInstance().deleteAllData()

        val webViewDatabase = WebViewDatabase.getInstance(context)
        webViewDatabase.clearFormData() // Unclear how this differs from WebView.clearFormData()
        webViewDatabase.clearHttpAuthUsernamePassword()
    }
}

private fun initWebview(webView: FirefoxWebView) = with(webView) {
    isVerticalScrollBarEnabled = true
    isHorizontalScrollBarEnabled = true

    if (BuildConfig.DEBUG) {
        setWebContentsDebuggingEnabled(true)
    }
}

@SuppressLint("SetJavaScriptEnabled") // We explicitly want to enable JavaScript
@Suppress("DEPRECATION") // To be safe, we'll use delete methods as long as they're there.
private fun initWebSettings(context: Context, settings: WebSettings) = with(settings) {
    val appName = context.resources.getString(R.string.useragent_appname)
    userAgentString = UserAgent.buildUserAgentString(context, settings, appName)

    javaScriptEnabled = true
    domStorageEnabled = true

    // The default for those settings should be "false" - But we want to be explicit.
    setAppCacheEnabled(false)
    databaseEnabled = false
    javaScriptCanOpenWindowsAutomatically = false

    saveFormData = false
    savePassword = false

    setGeolocationEnabled(false) // We do not implement the callbacks

    builtInZoomControls = true
    displayZoomControls = false // Hide by default

    loadWithOverviewMode = true // To respect the html viewport

    // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
    // Chrome does this in the current Chrome Dev, but not Chrome release).
    // TODO #33: TEXT_AUTOSIZING does not exist in AmazonWebSettings
    // settings.setLayoutAlgorithm(AmazonWebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

    // Consult with product before modifying these: for security reasons, we disable access to
    // arbitrary local files by webpages. Note: assets and resources can still be loaded via
    // file:///android_asset/...
    allowFileAccess = false
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false
    allowContentAccess = false
}

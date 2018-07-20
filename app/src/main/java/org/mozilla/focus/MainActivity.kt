/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_browser.*
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.browser.BrowserFragment.Companion.APP_URL_HOME
import org.mozilla.focus.ext.toSafeIntent
import org.mozilla.focus.home.pocket.Pocket
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.iwebview.WebViewProvider
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.Source
import org.mozilla.focus.telemetry.SentryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.toolbar.NavigationEvent
import org.mozilla.focus.toolbar.ToolbarCallbacks
import org.mozilla.focus.toolbar.ToolbarIntegration
import org.mozilla.focus.toolbar.ToolbarStateProvider
import org.mozilla.focus.utils.OnUrlEnteredListener
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.utils.publicsuffix.PublicSuffix
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.browser.BrowserFragmentCallbacks

class MainActivity : LocaleAwareAppCompatActivity(), OnUrlEnteredListener, BrowserFragmentCallbacks {

    private val sessionManager = SessionManager.getInstance()

    private val fragmentLifecycleCallbacks = MainActivityFragmentLifecycleCallbacks()
    private val browserFragment: BrowserFragment? get() =
        supportFragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?

    private lateinit var toolbarCallbacks: ToolbarCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable crash reporting. Don't add anything above here because if it crashes, we won't know.
        SentryWrapper.init(this)
        Pocket.init()
        PublicSuffix.init(this) // Used by Pocket Video feed & custom home tiles.

        val intent = SafeIntent(intent)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        IntentValidator.validateOnCreate(this, intent, savedInstanceState, ::onValidBrowserIntent)
        sessionManager.sessions.observe(this, object : NonNullObserver<List<Session>>() {
            public override fun onValueChanged(value: List<Session>) {
                val sessions = value
                if (sessions.isEmpty()) {
                    // There's no active session. Start a new session with "homepage".
                    ScreenController.showBrowserScreenForUrl(supportFragmentManager, APP_URL_HOME, Source.NONE)
                } else {
                    ScreenController.showBrowserScreenForCurrentSession(supportFragmentManager, sessionManager)
                }
            }
        })

        if (Settings.getInstance(this@MainActivity).shouldShowOnboarding()) {
            val onboardingIntent = Intent(this@MainActivity, OnboardingActivity::class.java)
            startActivity(onboardingIntent)
        }

        initViews()
        WebViewProvider.preload(this)
        toolbarCallbacks = ToolbarIntegration.setup(toolbar, DelegateToBrowserToolbarStateProvider(), ::onToolbarEvent)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
    }

    private fun initViews() {
        appBarOverlay.setOnClickListener {
            appBarOverlay.visibility = View.GONE
            browserFragment?.setOverlayVisibleByUser(false, toAnimate = true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    }

    override fun onNewIntent(unsafeIntent: Intent) {
        IntentValidator.validate(this, unsafeIntent.toSafeIntent(), ::onValidBrowserIntent)
    }

    private fun onValidBrowserIntent(url: String, source: Source) {
        ScreenController.showBrowserScreenForUrl(supportFragmentManager, url, source)
    }

    override fun applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    override fun onResume() {
        super.onResume()
        TelemetryWrapper.startSession(this)
    }

    override fun onPause() {
        super.onPause()
        TelemetryWrapper.stopSession(this)
    }

    override fun onStart() {
        super.onStart()
        Pocket.startBackgroundUpdates()
    }

    override fun onStop() {
        super.onStop()
        Pocket.stopBackgroundUpdates() // Don't regularly hit the network in the background.
        TelemetryWrapper.stopMainActivity()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == IWebView::class.java.name) {
            // Inject our implementation of IWebView from the WebViewProvider.
            WebViewProvider.create(this, attrs)
        } else super.onCreateView(name, context, attrs)
    }

    override fun onBackPressed() {
        val browserFragment = browserFragment
        if (browserFragment != null &&
                browserFragment.isVisible &&
                browserFragment.onBackPressed()) {
            // The Browser fragment handles back presses on its own because it might just go back
            // in the browsing history.
            return
        }
        super.onBackPressed()
    }

    override fun onNonTextInputUrlEntered(urlStr: String) {
        ViewUtils.hideKeyboard(container)
        ScreenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, false,
                null, null)
    }

    override fun onTextInputUrlEntered(urlStr: String,
                                       autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?,
                                       inputLocation: UrlTextInputLocation?) {
        ViewUtils.hideKeyboard(container)
        // It'd be much cleaner/safer to do this with a kotlin callback.
        ScreenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, true,
                autocompleteResult, inputLocation)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val browserFragment = browserFragment
        return if (browserFragment != null && browserFragment.isVisible) {
            browserFragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    private fun onToolbarEvent(event: NavigationEvent, value: String?, autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) {
        when (event) {
            NavigationEvent.SETTINGS -> {
//                Disabled as part of #129
//                ScreenController.showSettingsScreen(fragmentManager)
                return
            }

            NavigationEvent.TURBO -> Settings.getInstance(this).isBlockingEnabled = value == NavigationEvent.VAL_CHECKED
            else -> Unit // Do nothing.
        }

        val browserFragment = browserFragment
        if (browserFragment != null && browserFragment.isVisible) {
            browserFragment.onNavigationEvent(event, value, autocompleteResult)
        } // BrowserFragment is our only fragment: this else case should never happen.
    }

    override fun onHomeVisibilityChange(isHomeVisible: Boolean, isHomescreenOnStartup: Boolean) {
        // If this is the homescreen we show on startup, we want the user to be able to interact with
        // the toolbar and be unable to dismiss the home page (which has no content behind it). If
        // is another homescreen, we overlay the toolbar to prevent interacting with it and allow
        // dismissing, to show the web content, when clicked.
        appBarOverlay.visibility = if (isHomeVisible && !isHomescreenOnStartup) View.VISIBLE else View.GONE
    }

    override fun onFullScreenChange(isFullscreen: Boolean) {
        appBarLayout.setExpanded(!isFullscreen, true) // Not expanded means hidden.
    }

    private inner class MainActivityFragmentLifecycleCallbacks : FragmentLifecycleCallbacks() {
        override fun onFragmentAttached(fragmentManager: FragmentManager, fragment: Fragment, context: Context) {
            if (fragment is BrowserFragment) {
                fragment.onUrlUpdate = toolbarCallbacks.onDisplayUrlUpdate
                fragment.onSessionProgressUpdate = toolbarCallbacks.onProgressUpdate
            }
        }
    }

    private inner class DelegateToBrowserToolbarStateProvider : ToolbarStateProvider {
        private fun getBrowserToolbarProvider() =
                browserFragment?.toolbarStateProvider

        override fun isBackEnabled() = getBrowserToolbarProvider()?.isBackEnabled() ?: false
        override fun isForwardEnabled() = getBrowserToolbarProvider()?.isForwardEnabled() ?: false
        override fun getCurrentUrl() = getBrowserToolbarProvider()?.getCurrentUrl()
        override fun isURLPinned() = getBrowserToolbarProvider()?.isURLPinned() ?: false
        override fun isPinEnabled() = getBrowserToolbarProvider()?.isPinEnabled() ?: false
        override fun isRefreshEnabled() = getBrowserToolbarProvider()?.isRefreshEnabled() ?: false
    }
}

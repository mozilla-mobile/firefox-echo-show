/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import mozilla.components.support.utils.SafeIntent
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.animation.VisibilityAnimator
import org.mozilla.focus.architecture.FirefoxViewModelProviders
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.browser.BrowserFragmentCallbacks
import org.mozilla.focus.ext.getBrowserFragment
import org.mozilla.focus.ext.getNavigationOverlay
import org.mozilla.focus.ext.isVisibleAndNonNull
import org.mozilla.focus.ext.toSafeIntent
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.iwebview.WebViewProvider
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.Source
import org.mozilla.focus.settings.SettingsActivity
import org.mozilla.focus.settings.UserClearDataEvent
import org.mozilla.focus.settings.UserClearDataEventObserver
import org.mozilla.focus.telemetry.SentryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.toolbar.BrowserAppBarLayoutController
import org.mozilla.focus.toolbar.BrowserAppBarViewModel
import org.mozilla.focus.toolbar.ToolbarCallbacks
import org.mozilla.focus.toolbar.ToolbarEvent
import org.mozilla.focus.toolbar.ToolbarIntegration
import org.mozilla.focus.toolbar.ToolbarStateProvider
import org.mozilla.focus.toolbar.ToolbarViewModel
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.utils.publicsuffix.PublicSuffix

class MainActivity : LocaleAwareAppCompatActivity(), BrowserFragmentCallbacks, UrlSearcher {

    private val sessionManager = SessionManager.getInstance()

    private lateinit var toolbarCallbacks: ToolbarCallbacks
    private val toolbarStateProvider = DelegateToBrowserToolbarStateProvider()
    private lateinit var appBarLayoutController: BrowserAppBarLayoutController

    private val toolbarViewModel: ToolbarViewModel
        get() = FirefoxViewModelProviders.of(this)[ToolbarViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable crash reporting. Don't add anything above here because if it crashes, we won't know.
        SentryWrapper.init(this)
        PublicSuffix.init(this) // Used by custom home tiles.

        val intent = SafeIntent(intent)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        IntentValidator.validateOnCreate(this, intent, savedInstanceState, ::onValidBrowserIntent)
        sessionManager.sessions.observe(this, object : NonNullObserver<List<Session>>() {
            public override fun onValueChanged(value: List<Session>) {
                val sessions = value
                if (sessions.isEmpty()) {
                    ScreenController.showBrowserScreenForStartupHomeScreen(supportFragmentManager)
                } else {
                    ScreenController.showBrowserScreenForCurrentSession(supportFragmentManager, sessionManager)
                }
            }
        })

        /*
        Temporarily removing Turbo Mode for breaking sites.
        if (Settings.getInstance(this@MainActivity).shouldShowOnboarding()) {
            val onboardingIntent = Intent(this@MainActivity, OnboardingActivity::class.java)
            startActivity(onboardingIntent)
        }
        */

        initViews()
        WebViewProvider.preload(this)
        UserClearDataEvent.liveData.observe(this, UserClearDataEventObserver(this))
    }

    private fun initViews() {
        FirefoxViewModelProviders.of(this)[BrowserAppBarViewModel::class.java].let { viewModel ->
            appBarLayoutController = BrowserAppBarLayoutController(viewModel, appBarLayout, toolbar).apply {
                init(this@MainActivity)
            }
        }

        toolbarCallbacks = ToolbarIntegration.setup(this, toolbarViewModel, toolbar, toolbarStateProvider,
            ::onToolbarEvent)
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
        TelemetryWrapper.startSession()
    }

    override fun onPause() {
        super.onPause()
        TelemetryWrapper.stopSession()
    }

    override fun onStop() {
        super.onStop()
        TelemetryWrapper.stopMainActivity()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == IWebView::class.java.name) {
            // Inject our implementation of IWebView from the WebViewProvider.
            WebViewProvider.create(this, attrs)
        } else super.onCreateView(name, context, attrs)
    }

    override fun onNonTextInputUrlEntered(urlStr: String) {
        ViewUtils.hideKeyboard(container)
        ScreenController.onUrlEnteredInner(this, supportFragmentManager, urlStr)
    }

    override fun onTextInputUrlEntered(
        urlStr: String,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult
    ) {
        ViewUtils.hideKeyboard(container)
        ScreenController.onUrlEnteredInner(this, supportFragmentManager, urlStr) { isUrl ->
            TelemetryWrapper.urlBarEvent(isUrl, autocompleteResult)
        }
    }

    private fun onToolbarEvent(event: ToolbarEvent, value: String?, autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) {
        if (event == ToolbarEvent.HOME && supportFragmentManager.getNavigationOverlay().isVisibleAndNonNull) {
            // The home button does nothing on when home is visible.
            return
        } else if (event == ToolbarEvent.LOAD_URL) {
            onTextInputUrlEntered(value!!, autocompleteResult!!)
            return // Telemetry is handled elsewhere.
        }

        TelemetryWrapper.toolbarEvent(event, value)

        val browserFragment = supportFragmentManager.getBrowserFragment()
        when (event) {
            ToolbarEvent.SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))
//            ToolbarEvent.TURBO -> Settings.getInstance(this).isBlockingEnabled = value == ToolbarEvent.VAL_CHECKED

            // BrowserFragment is our only fragment so no other fragment should ever be visible.
            else -> if (browserFragment?.isVisible == true) { browserFragment.onToolbarEvent(event, value) }
        }
    }

    override fun setNavigationOverlayIsVisible(isVisible: Boolean, isOverlayOnStartup: Boolean) {
        ScreenController.setNavigationOverlayIsVisible(supportFragmentManager, appBarLayoutController, toolbarViewModel,
            isVisible = isVisible, isOverlayOnStartup = isOverlayOnStartup)
    }

    override fun onUrlUpdate(url: String?) = toolbarCallbacks.onDisplayUrlUpdate(url)
    override fun onSessionLoadingUpdate(isLoading: Boolean) = toolbarCallbacks.onLoadingUpdate(isLoading)
    override fun onSessionProgressUpdate(progress: Int) = toolbarCallbacks.onProgressUpdate(progress)

    private inner class DelegateToBrowserToolbarStateProvider : ToolbarStateProvider {
        private fun getBrowserToolbarProvider() =
                supportFragmentManager.getBrowserFragment()?.toolbarStateProvider

        override fun isBackEnabled() = getBrowserToolbarProvider()?.isBackEnabled() ?: false
        override fun isForwardEnabled() = getBrowserToolbarProvider()?.isForwardEnabled() ?: false
        override fun getCurrentUrl() = getBrowserToolbarProvider()?.getCurrentUrl()
        override fun isURLPinned() = getBrowserToolbarProvider()?.isURLPinned() ?: false
        override fun isStartupHomepageVisible() = getBrowserToolbarProvider()?.isStartupHomepageVisible() ?: false
    }

    override fun onHomeTileLongClick(unpinTile: () -> Unit) {
        unpinButton.setOnClickListener {
            unpinTile()
            VisibilityAnimator.animateVisibility(unpinOverlay, false)
        }
        unpinOverlay.setOnClickListener {
            VisibilityAnimator.animateVisibility(unpinOverlay, false)
        }

        // In some cases, the VisibilityAnimator would not start so we replaced it with this.
        unpinOverlay.visibility = View.VISIBLE
        unpinOverlay.alpha = 0f
        unpinOverlay.animate()
                .setDuration(150)
                .alpha(1f)
                .start()
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.annotation.DrawableRes
import android.view.View
import mozilla.components.browser.domains.DomainAutoCompleteProvider
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.support.ktx.android.view.dp
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.R
import org.mozilla.focus.TouchInterceptorLayout
import org.mozilla.focus.browser.BrowserFragment.Companion.APP_URL_STARTUP_HOME
import org.mozilla.focus.toolbar.ToolbarEvent.* // ktlint-disable no-wildcard-imports

enum class ToolbarEvent {
    HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD_URL, TURBO, PIN_ACTION;

    companion object {
        const val VAL_CHECKED = "checked"
        const val VAL_UNCHECKED = "unchecked"
    }
}

/** A collection of callbacks to modify the toolbar. */
class ToolbarCallbacks(
        val onDisplayUrlUpdate: (url: String?) -> Unit,
        val onLoadingUpdate: (isLoading: Boolean) -> Unit,
        val onProgressUpdate: (progress: Int) -> Unit
)

typealias OnToolbarEvent = (event: ToolbarEvent, value: String?,
                            autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) -> Unit

/**
 * Helper class for constructing and using the shared toolbar for navigation and homescreen.
 */
object ToolbarIntegration {

    /**
     * A map that keeps strong references to [OnSharedPreferenceChangeListener]s until the object it
     * manipulates, [BrowserToolbar], is GC'd (i.e. their lifecycles are the same). This is necessary
     * because [SharedPreferences.registerOnSharedPreferenceChangeListener] doesn't keep strong
     * references so someone else, this object, has to.
     */
    //private val weakToolbarToSharedPrefListeners = WeakHashMap<BrowserToolbar, OnSharedPreferenceChangeListener>()

    /**
     * Add the components of toolbar and returns a collection of callbacks to modify the toolbar
     * at runtime.
     *
     * We return callbacks, rather than the internal toolbar views, because it allows us to:
     * - Group all the low-level toolbar logic in this file
     * - Put all toolbar interactions behind an "interface" rather than coupling code to raw toolbar views
     * - Make the code more testable, due to the "interface" ^
     */
    @SuppressWarnings("LongMethod")
    fun setup(toolbar: BrowserToolbar,
              toolbarStateProvider: ToolbarStateProvider,
              onToolbarEvent: OnToolbarEvent): ToolbarCallbacks {
        val context = toolbar.context

        toolbar.displaySiteSecurityIcon = false
        toolbar.hint = toolbar.context.getString(R.string.urlbar_hint)

        configureToolbarSpacing(toolbar)
        initTextChangeListeners(context, toolbar, onToolbarEvent)

        val progressBar = UrlBoxProgressView(context)
        toolbar.urlBoxView = progressBar
        val progressBarController = ProgressBarController(progressBar)

        val pinButton = addToolbarButtons(context, toolbar, toolbarStateProvider, onToolbarEvent)

        return ToolbarCallbacks(
                onDisplayUrlUpdate = { url -> onDisplayUrlUpdate(toolbar, toolbarStateProvider, url, pinButton) },
                onLoadingUpdate = progressBarController::onLoadingUpdate,
                onProgressUpdate = progressBarController::onProgressUpdate
        )
    }

    private fun configureToolbarSpacing(toolbar: BrowserToolbar) {
        val dp16 = toolbar.dp(16)
        val dp48 = toolbar.dp(48)
        val dp24 = toolbar.dp(24)

        toolbar.setPadding(dp48, dp24, dp48, dp24)
        toolbar.urlBoxMargin = dp16
        toolbar.setUrlTextPadding(dp16, dp16, dp16, dp16)
        toolbar.browserActionMargin = dp16
    }

    private fun addToolbarButtons(
            context: Context,
            toolbar: BrowserToolbar,
            toolbarStateProvider: ToolbarStateProvider,
            onToolbarEvent: OnToolbarEvent
    ): ChangeableVisibilityButton {
        val homescreenButton = Toolbar.ActionButton(R.drawable.ic_grid,
                context.getString(R.string.homescreen_title)) { onToolbarEvent(HOME, null, null) }
        toolbar.addNavigationAction(homescreenButton)

        val backButton = BrowserToolbar.Button(R.drawable.ic_back,
                context.getString(R.string.content_description_back),
                background = R.drawable.toolbar_button_background,
                visible = toolbarStateProvider::isBackEnabled) { onToolbarEvent(BACK, null, null) }
        toolbar.addNavigationAction(backButton)

        val forwardButton = BrowserToolbar.Button(R.drawable.ic_forward,
                context.getString(R.string.content_description_forward),
                toolbarStateProvider::isForwardEnabled,
                background = R.drawable.toolbar_button_background) { onToolbarEvent(FORWARD, null, null) }
        toolbar.addNavigationAction(forwardButton)

        val refreshButton = BrowserToolbar.Button(R.drawable.ic_refresh,
                context.getString(R.string.content_description_reload),
                background = R.drawable.toolbar_button_background,
                visible = { !toolbarStateProvider.isStartupHomepageVisible() }) { onToolbarEvent(RELOAD, null, null) }
        toolbar.addPageAction(refreshButton)

        val pinButton = ChangeableVisibilityButton(imageResource = R.drawable.ic_pin,
                imageResourceSelected = R.drawable.ic_pin_filled,
                contentDescription = context.getString(R.string.pin_label),
                contentDescriptionSelected = context.getString(R.string.homescreen_unpin_a11y),
                background = R.drawable.toolbar_toggle_background,
                visibility = {
                    if (!toolbarStateProvider.isStartupHomepageVisible()) View.VISIBLE
                    else View.INVISIBLE
                }) { isSelected ->
            onToolbarEvent(PIN_ACTION, if (isSelected) ToolbarEvent.VAL_CHECKED else ToolbarEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(pinButton)

        /*
        val turboButton = BrowserToolbar.ToggleButton(imageResource = R.drawable.ic_rocket,
                imageResourceSelected = R.drawable.ic_rocket_filled,
                contentDescription = context.getString(R.string.turbo_mode_enable_a11y),
                contentDescriptionSelected = context.getString(
                        R.string.turbo_mode_disable_a11y),
                background = R.drawable.toolbar_toggle_background,
                selected = Settings.getInstance(toolbar.context).isBlockingEnabled) { isSelected ->
            onToolbarEvent(TURBO, if (isSelected) ToolbarEvent.VAL_CHECKED else ToolbarEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(turboButton)
        */
        toolbar.addBrowserAction(Toolbar.ActionSpace(toolbar.dp(192)))

        val settingsButton = BrowserToolbar.Button(R.drawable.ic_settings,
                context.getString(R.string.menu_settings),
                background = R.drawable.toolbar_button_background) {
            onToolbarEvent(SETTINGS, null, null)
        }
        toolbar.addBrowserAction(settingsButton)

        val brandIcon = Toolbar.ActionImage(R.drawable.ic_firefox_and_workmark,
                "")
        toolbar.addBrowserAction(brandIcon)

        /*
        val sharedPrefsListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == IWebView.TRACKING_PROTECTION_ENABLED_PREF) {
                turboButton.setSelected(sharedPreferences.getBoolean(key, true /* unused */),
                        notifyListener = true) // Allows BrowserFragment to respond.
            }
        }
        Settings.getInstance(toolbar.context).preferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        weakToolbarToSharedPrefListeners[toolbar] = sharedPrefsListener
        */
        return pinButton
    }

    private fun initTextChangeListeners(context: Context, toolbar: BrowserToolbar,
                                        onToolbarEvent: OnToolbarEvent) {
        val domainAutoCompleteProvider = DomainAutoCompleteProvider().apply {
            initialize(context)
        }
        toolbar.setAutocompleteFilter { value, view ->
            view?.let {
                val suggestion = domainAutoCompleteProvider.autocomplete(value)
                view.applyAutocompleteResult(
                        InlineAutocompleteEditText.AutocompleteResult(suggestion.text,
                                suggestion.source, suggestion.size, { suggestion.url }))
            }
        }

        toolbar.setOnUrlCommitListener { urlStr ->
            val result = domainAutoCompleteProvider.autocomplete(urlStr)
            val autocompleteResult = InlineAutocompleteEditText.AutocompleteResult(result.text, result.source, result.size)
            onToolbarEvent(LOAD_URL, urlStr, autocompleteResult)
        }

        toolbar.rootView?.findViewById<TouchInterceptorLayout>(R.id.main_content)
                ?.setOnTouchOutsideViewListener(toolbar) {
                    toolbar.displayMode()
                }
    }
}

private fun onDisplayUrlUpdate(
        toolbar: BrowserToolbar, toolbarStateProvider: ToolbarStateProvider, url: String?,
        pinButton: Toolbar.ActionToggleButton
) {
    toolbar.url = when (url) {
        APP_URL_STARTUP_HOME -> "" // Uses hint instead
        null -> toolbar.url
        else -> url
    }

    pinButton.setSelected(toolbarStateProvider.isURLPinned(),
            notifyListener = false) // We don't want to actually pin/unpin.
    toolbar.invalidateActions()
}

/**
 * A [BrowserToolbar.ToggleButton] that can be set to different visibilities
 */
private class ChangeableVisibilityButton(
        @DrawableRes imageResource: Int,
        @DrawableRes imageResourceSelected: Int,
        contentDescription: String,
        contentDescriptionSelected: String,
        @DrawableRes background: Int? = null,
        val visibility: () -> Int,
        listener: (Boolean) -> Unit
) : BrowserToolbar.ToggleButton(
        imageResource,
        imageResourceSelected,
        contentDescription,
        contentDescriptionSelected,
        background = background,
        listener = listener
) {
    override fun bind(view: View) {
        super.bind(view)
        view.visibility = visibility()
    }
}

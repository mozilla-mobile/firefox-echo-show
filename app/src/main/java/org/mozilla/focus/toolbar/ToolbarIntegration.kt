/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.focus.toolbar.NavigationEvent.* // ktlint-disable no-wildcard-imports
import org.mozilla.focus.R
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.ext.setSelected
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.widget.InlineAutocompleteEditText
import java.util.WeakHashMap

enum class NavigationEvent {
    HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD_URL, LOAD_TILE, TURBO, PIN_ACTION;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.navButtonSettings -> SETTINGS
            R.id.turboButton -> TURBO
            R.id.pinButton -> PIN_ACTION
            else -> null
        }

        const val VAL_CHECKED = "checked"
        const val VAL_UNCHECKED = "unchecked"
    }
}

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
    private val weakToolbarToSharedPrefListeners = WeakHashMap<BrowserToolbar, OnSharedPreferenceChangeListener>()

    /**
     * Add the components of toolbar.
     *
     * This method is assumed to be idempotent: see [MainActivityFragmentLifecycleCallbacks].
     */
    @SuppressWarnings("LongMethod")
    fun setup(toolbar: BrowserToolbar,
              navigationStateProvider: BrowserFragment.NavigationStateProvider,
              onToolbarEvent: (event: NavigationEvent, value: String?,
                               autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) -> Unit) {
        val context = toolbar.context

        toolbar.displaySiteSecurityIcon = false

        toolbar.setPadding(48, 24, 48, 24)
        toolbar.urlBoxMargin = 16
        toolbar.setUrlTextPadding(16, 16, 16, 16)
        toolbar.hint = toolbar.context.getString(R.string.urlbar_hint)

        toolbar.setOnUrlChangeListener { urlStr ->
            // TODO: #86 - toolbar doesn't support autocomplete yet so we pass in a dummy result.
            val autocompleteResult = InlineAutocompleteEditText.AutocompleteResult("", "", 0)
            onToolbarEvent(LOAD_URL, urlStr, autocompleteResult)
        }

        val homescreenButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_grid,
                "Homescreen") { onToolbarEvent(HOME, null, null) }
        toolbar.addNavigationAction(homescreenButton)

        val backButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_back,
                context.getString(R.string.content_description_back),
                visible = navigationStateProvider::isBackEnabled) { onToolbarEvent(BACK, null, null) }
        toolbar.addNavigationAction(backButton)

        val forwardButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_forward,
                context.getString(R.string.content_description_forward),
                navigationStateProvider::isForwardEnabled) { onToolbarEvent(FORWARD, null, null) }
        toolbar.addNavigationAction(forwardButton)

        val refreshButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_refresh,
                context.getString(R.string.content_description_reload),
                visible = navigationStateProvider::isRefreshEnabled) { onToolbarEvent(RELOAD, null, null) }
        toolbar.addPageAction(refreshButton)

        val pinButton = Toolbar.ActionToggleButton(imageResource = R.drawable.pin_unfilled,
                imageResourceSelected = R.drawable.pin_filled,
                contentDescription = context.getString(R.string.pin_label),
                contentDescriptionSelected = "Unpin",
                visible = navigationStateProvider::isPinEnabled) { isSelected ->
            onToolbarEvent(PIN_ACTION, if (isSelected) NavigationEvent.VAL_CHECKED else NavigationEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(pinButton)

        val turboButton = Toolbar.ActionToggleButton(imageResource = R.drawable.turbo_off,
                imageResourceSelected = R.drawable.turbo_on,
                contentDescription = context.getString(R.string.turbo_mode),
                contentDescriptionSelected = context.getString(
                        R.string.onboarding_turbo_mode_button_off),
                selected = Settings.getInstance(toolbar.context).isBlockingEnabled) { isSelected ->
            onToolbarEvent(TURBO, if (isSelected) NavigationEvent.VAL_CHECKED else NavigationEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(turboButton)

        val settingsButton = Toolbar.ActionButton(R.drawable.ic_settings,
                context.getString(R.string.menu_settings)) {
            onToolbarEvent(SETTINGS, null, null)
        }
        toolbar.addBrowserAction(settingsButton)

        val brandIcon = Toolbar.ActionImage(R.drawable.ic_firefox_and_workmark,
                "")
        toolbar.addBrowserAction(brandIcon)

        val sharedPrefsListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == IWebView.TRACKING_PROTECTION_ENABLED_PREF) {
                turboButton.setSelected(sharedPreferences.getBoolean(key, true /* unused */))
            }
        }
        Settings.getInstance(toolbar.context).preferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        weakToolbarToSharedPrefListeners[toolbar] = sharedPrefsListener
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.view.View
import android.view.ViewGroup
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.support.ktx.android.view.dp
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.focus.toolbar.NavigationEvent.* // ktlint-disable no-wildcard-imports
import org.mozilla.focus.R
import org.mozilla.focus.browser.BrowserFragment.Companion.APP_URL_HOME
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

/** A collection of callbacks to modify the toolbar. */
class ToolbarCallbacks(
        val onDisplayUrlUpdate: (url: String?) -> Unit,
        val onProgressUpdate: (progress: Int) -> Unit
)

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
              onToolbarEvent: (event: NavigationEvent, value: String?,
                               autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) -> Unit): ToolbarCallbacks {
        val context = toolbar.context

        toolbar.displaySiteSecurityIcon = false

        val dp16 = toolbar.dp(16)
        val dp48 = toolbar.dp(48)
        val dp24 = toolbar.dp(24)

        toolbar.setPadding(dp48, dp24, dp48, dp24)
        toolbar.urlBoxMargin = dp16
        toolbar.setUrlTextPadding(dp16, dp16, dp16, dp16)
        toolbar.browserActionMargin = dp16
        toolbar.hint = toolbar.context.getString(R.string.urlbar_hint)

        toolbar.setOnUrlChangeListener { urlStr ->
            // TODO: #86 - toolbar doesn't support autocomplete yet so we pass in a dummy result.
            val autocompleteResult = InlineAutocompleteEditText.AutocompleteResult("", "", 0)
            onToolbarEvent(LOAD_URL, urlStr, autocompleteResult)
        }

        val progressBar = UrlBoxProgressView(context)
        toolbar.urlBoxView = progressBar
        toolbar.urlBoxMargin = dp16

        val homescreenButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_grid,
                context.getString(R.string.homescreen_title)) { onToolbarEvent(HOME, null, null) }
        toolbar.addNavigationAction(homescreenButton)

        val backButton = BrowserToolbar.Button(iconsR.drawable.mozac_ic_back,
                context.getString(R.string.content_description_back),
                background = R.drawable.toolbar_button_background,
                visible = toolbarStateProvider::isBackEnabled) { onToolbarEvent(BACK, null, null) }
        toolbar.addNavigationAction(backButton)

        val forwardButton = BrowserToolbar.Button(iconsR.drawable.mozac_ic_forward,
                context.getString(R.string.content_description_forward),
                toolbarStateProvider::isForwardEnabled,
                background = R.drawable.toolbar_button_background) { onToolbarEvent(FORWARD, null, null) }
        toolbar.addNavigationAction(forwardButton)

        val refreshButton = BrowserToolbar.Button(iconsR.drawable.mozac_ic_refresh,
                context.getString(R.string.content_description_reload),
                background = R.drawable.toolbar_button_background,
                visible = toolbarStateProvider::isRefreshEnabled) { onToolbarEvent(RELOAD, null, null) }
        toolbar.addPageAction(refreshButton)

        val pinButton = BrowserToolbar.ToggleButton(imageResource = iconsR.drawable.mozac_ic_pin,
                imageResourceSelected = iconsR.drawable.mozac_ic_pin_filled,
                contentDescription = context.getString(R.string.pin_label),
                contentDescriptionSelected = context.getString(R.string.homescreen_unpin_a11y),
                background = R.drawable.toolbar_toggle_background,
                visible = toolbarStateProvider::isPinEnabled) { isSelected ->
            onToolbarEvent(PIN_ACTION, if (isSelected) NavigationEvent.VAL_CHECKED else NavigationEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(pinButton)

        val turboButton = BrowserToolbar.ToggleButton(imageResource = iconsR.drawable.mozac_ic_rocket,
                imageResourceSelected = iconsR.drawable.mozac_ic_rocket_filled,
                contentDescription = context.getString(R.string.turbo_mode_enable_a11y),
                contentDescriptionSelected = context.getString(
                        R.string.turbo_mode_disable_a11y),
                background = R.drawable.toolbar_toggle_background,
                selected = Settings.getInstance(toolbar.context).isBlockingEnabled) { isSelected ->
            onToolbarEvent(TURBO, if (isSelected) NavigationEvent.VAL_CHECKED else NavigationEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(turboButton)

        toolbar.addBrowserAction(DynamicSpace(
            toolbar.dp(160) - 2 * toolbar.browserActionMargin,
            toolbar.dp(88) - 2 * toolbar.browserActionMargin,
            toolbarStateProvider::isPinEnabled
        ))

        val settingsButton = BrowserToolbar.Button(R.drawable.ic_settings,
                context.getString(R.string.menu_settings),
                background = R.drawable.toolbar_button_background) {
            onToolbarEvent(SETTINGS, null, null)
        }
        toolbar.addBrowserAction(settingsButton)

        val brandIcon = Toolbar.ActionImage(R.drawable.ic_firefox_and_workmark,
                "")
        toolbar.addBrowserAction(brandIcon)

        val sharedPrefsListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == IWebView.TRACKING_PROTECTION_ENABLED_PREF) {
                turboButton.setSelected(sharedPreferences.getBoolean(key, true /* unused */),
                        notifyListener = true) // Allows BrowserFragment to respond.
            }
        }
        Settings.getInstance(toolbar.context).preferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        weakToolbarToSharedPrefListeners[toolbar] = sharedPrefsListener

        return ToolbarCallbacks(
                onDisplayUrlUpdate = { url -> onDisplayUrlUpdate(toolbar, toolbarStateProvider, url, pinButton) },
                onProgressUpdate = { progress -> progressBar.progress = progress }
        )
    }
}

private fun onDisplayUrlUpdate(
        toolbar: BrowserToolbar, toolbarStateProvider: ToolbarStateProvider, url: String?,
        pinButton: Toolbar.ActionToggleButton
) {
    toolbar.url = when (url) {
        APP_URL_HOME -> "" // Uses hint instead
        null -> toolbar.url
        else -> url
    }

    pinButton.setSelected(toolbarStateProvider.isURLPinned(),
            notifyListener = false) // We don't want to actually pin/unpin.
    toolbar.invalidateActions()
}

/**
 * An "empty" toolbar action that just displays nothing and resizes dynamically based on whether
 * the pin icon is displayed or not.
 */
private class DynamicSpace(
        private val largeWidth: Int,
        private val smallWidth: Int,
        private val showSmallWidth: () -> Boolean
) : Toolbar.Action {
    override fun createView(parent: ViewGroup): View = View(parent.context).apply {
        minimumWidth = largeWidth
    }

    override fun bind(view: View) {
        if (showSmallWidth.invoke()) {
            view.minimumWidth = smallWidth
        } else {
            view.minimumWidth = largeWidth
        }
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.focus.R

enum class NavigationEvent {
    SETTINGS, BACK, FORWARD, RELOAD, LOAD_URL, LOAD_TILE, TURBO, PIN_ACTION;

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
    /*
     * Add the components of toolbar.
     */
    @SuppressWarnings("LongMethod")
    fun setup(toolbar: BrowserToolbar) {
        val context = toolbar.context

        toolbar.displaySiteSecurityIcon = false

        toolbar.setPadding(48, 24, 48, 24)
        toolbar.urlBoxMargin = 16
        toolbar.setUrlTextPadding(16, 16, 16, 16)
        toolbar.hint = toolbar.context.getString(R.string.urlbar_hint)

        val homescreenButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_grid,
                "Homescreen") {}
        toolbar.addNavigationAction(homescreenButton)

        val backButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_back,
                context.getString(R.string.content_description_back)) {}
        toolbar.addNavigationAction(backButton)

        val forwardButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_forward,
                context.getString(R.string.content_description_forward)) {}
        toolbar.addNavigationAction(forwardButton)

        val refreshButton = Toolbar.ActionButton(iconsR.drawable.mozac_ic_refresh,
                context.getString(R.string.content_description_reload)) {}
        toolbar.addPageAction(refreshButton)

        val pinButton = Toolbar.ActionToggleButton(imageResource = R.drawable.pin_unfilled,
                imageResourceSelected = R.drawable.pin_filled,
                contentDescription = context.getString(R.string.pin_label),
                contentDescriptionSelected = "Unpin") {}
        toolbar.addBrowserAction(pinButton)

        val turboButton = Toolbar.ActionToggleButton(imageResource = R.drawable.turbo_off,
                imageResourceSelected = R.drawable.turbo_on,
                contentDescription = context.getString(R.string.turbo_mode),
                contentDescriptionSelected = context.getString(
                        R.string.onboarding_turbo_mode_button_off)) {}
        toolbar.addBrowserAction(turboButton)

        val settingsButton = Toolbar.ActionButton(R.drawable.ic_settings,
                context.getString(R.string.menu_settings)) {}
        toolbar.addBrowserAction(settingsButton)

        val brandIcon = Toolbar.ActionImage(R.drawable.ic_firefox_and_workmark,
                "")
        toolbar.addBrowserAction(brandIcon)
    }
}

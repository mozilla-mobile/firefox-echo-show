package org.mozilla.focus.toolbar

import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import org.mozilla.focus.R

object ToolbarHelper {
    /*
     * Add the components of toolbar.
     */
    fun setupToolbar(toolbar: BrowserToolbar) {
        toolbar.displaySiteSecurityIcon = false
        toolbar.url = "www.mozilla.org"

        val spacer = Toolbar.ActionSpace(20)
        toolbar.addNavigationAction(spacer)

        val grid = Toolbar.ActionButton(mozilla.components.ui.icons.R.drawable.mozac_ic_grid,
                "Homescreen") {}
        toolbar.addNavigationAction(grid)

        val backButton = Toolbar.ActionButton(mozilla.components.ui.icons.R.drawable.mozac_ic_back,
                "Back") {}
        toolbar.addNavigationAction(backButton)

        val forwardButton = Toolbar.ActionButton(mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
                "Forward") {}
        toolbar.addNavigationAction(forwardButton)

        val refreshButton = Toolbar.ActionButton(mozilla.components.ui.icons.R.drawable.mozac_ic_refresh,
                "Refresh") {}
        toolbar.addPageAction(refreshButton)

        val pinButton = Toolbar.ActionToggleButton(R.drawable.pin_unfilled,
                R.drawable.pin_filled,
                "Pin", "Pin") {}
        toolbar.addBrowserAction(pinButton)

        val turboButton = Toolbar.ActionToggleButton(R.drawable.turbo_on,
                R.drawable.turbo_off,
                "Turbo Mode", "Turbo Mode") {}
        toolbar.addBrowserAction(turboButton)

        toolbar.addBrowserAction(spacer)

        val settingsButton = Toolbar.ActionButton(R.drawable.ic_settings,
                "Settings") {}
        toolbar.addBrowserAction(settingsButton)

        val brandIcon = Toolbar.ActionImage(R.drawable.ic_firefox_and_workmark,
                "")
        toolbar.addBrowserAction(brandIcon)
    }
}

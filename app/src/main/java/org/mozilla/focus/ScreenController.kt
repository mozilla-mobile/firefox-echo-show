/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.support.v4.app.FragmentManager
import android.text.TextUtils
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.browser.URLs.APP_STARTUP_HOME
import org.mozilla.focus.ext.getBrowserFragment
import org.mozilla.focus.ext.getNavigationOverlay
import org.mozilla.focus.home.NavigationOverlayAnimations
import org.mozilla.focus.home.NavigationOverlayFragment
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.Source
import org.mozilla.focus.toolbar.BrowserAppBarLayoutController
import org.mozilla.focus.toolbar.ToolbarViewModel
import org.mozilla.focus.utils.UrlUtils

object ScreenController {
    /**
     * Loads the given url. If isTextInput is true, there should be no null parameters.
     */
    fun onUrlEnteredInner(
        context: Context,
        fragmentManager: FragmentManager,
        urlStr: String,
        onSuccess: ((isUrl: Boolean) -> Unit)? = null
    ) {
        if (TextUtils.isEmpty(urlStr.trim())) {
            return
        }

        val isUrl = UrlUtils.isUrl(urlStr)
        val updatedUrlStr = if (isUrl) UrlUtils.normalize(urlStr) else UrlUtils.createSearchUrl(context, urlStr)

        showBrowserScreenForUrl(fragmentManager, updatedUrlStr, Source.USER_ENTERED)
        onSuccess?.invoke(isUrl)
    }

    fun showBrowserScreenForCurrentSession(fragmentManager: FragmentManager, sessionManager: SessionManager) {
        val currentSession = sessionManager.currentSession

        if (fragmentManager.getBrowserFragment()?.session?.isSameAs(currentSession) == true) {
            // There's already a BrowserFragment displaying this session.
            return
        }

        // BrowserFragment is the base fragment, so don't add a backstack transaction.
        fragmentManager
                .beginTransaction()
                .replace(R.id.container,
                        BrowserFragment.createForSession(currentSession), BrowserFragment.FRAGMENT_TAG)
                .commit()
    }

    fun showBrowserScreenForStartupHomeScreen(fragmentManager: FragmentManager) {
        // HACK: we don't want the home screen in the back stack so we don't want to load a home
        // screen URL. However, there's no simple way to create a BrowserFragment without a URL
        // so we assign the startup url to a session but don't load it into the WebView. Unfortunately,
        // this means the code controlling the initial homescreen is in several places and fragile:
        // the best way to identify this code is to Find Usages on the startup url constant.
        //
        // We should fix this by rewriting how we manage sessions when we integrate the session
        // android component.
        showBrowserScreenForUrl(fragmentManager, APP_STARTUP_HOME.toString(), Source.NONE)
    }

    fun showBrowserScreenForUrl(fragmentManager: FragmentManager?, url: String, source: Source) {
        // This code is not correct:
        // - We only support one session but it creates a new session when there's no BrowserFragment
        // such as each time we open a URL from the home screen.
        // - It doesn't handle the case where the BrowserFragment is non-null but not
        // visible: this can happen when a BrowserFragment is in the back stack, e.g. if this
        // method is called from Settings.
        //
        // However, from a user perspective, the behavior is correct (e.g. back stack functions
        // correctly with multiple sessions).
        val browserFragment = fragmentManager?.getBrowserFragment()
        if (browserFragment?.isVisible == true) {
            // We can't call loadUrl on the Fragment until the view hierarchy is inflated so we check
            // for visibility in addition to existence.
            browserFragment.loadUrl(url)
        } else {
            SessionManager.getInstance().createSession(source, url)
        }
    }

    fun recreateBrowserScreen(fragmentManager: FragmentManager) {
        fragmentManager.getBrowserFragment()?.let { browserFragment ->
            fragmentManager.beginTransaction()
                    .remove(browserFragment)
                    .commitNow() // Synchronous so our session observers have the correct state.
        }

        // Activates the start up code path, which creates a new session if there are none.
        SessionManager.getInstance().removeAllSessions()
    }

    /**
     * Sets the navigation overlay visibility based on the given params.
     *
     * This method will throw if this method is called to set the nav overlay visible
     * but it is already visible and vice versa.
     */
    fun setNavigationOverlayIsVisible(
        fragmentManager: FragmentManager,
        appBarLayoutController: BrowserAppBarLayoutController,
        toolbarViewModel: ToolbarViewModel,
        isVisible: Boolean,
        isOverlayOnStartup: Boolean
    ) {
        fun getNavOverlay() = fragmentManager.getNavigationOverlay()

        if (isVisible) {
            val newOverlay = NavigationOverlayFragment.newInstance(isOverlayOnStartup = isOverlayOnStartup)
            fragmentManager.beginTransaction()
                .replace(R.id.navigationOverlayContainer, newOverlay, NavigationOverlayFragment.FRAGMENT_TAG)
                .commit()
        } else {
            getNavOverlay()?.let { existingOverlay ->
                NavigationOverlayAnimations.animateOut(existingOverlay) {
                    fragmentManager.beginTransaction()
                        .remove(existingOverlay)
                        .commit()
                }
            }
        }

        fragmentManager.getBrowserFragment()?.onNavigationOverlayVisibilityChange(isVisible)
        appBarLayoutController.onHomeVisibilityChange(isVisible)
        toolbarViewModel.onNavigationOverlayVisibilityChange(isVisible, isOverlayOnStartup)
    }
}

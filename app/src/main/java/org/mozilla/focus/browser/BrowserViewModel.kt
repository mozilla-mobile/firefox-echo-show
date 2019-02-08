/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import org.mozilla.focus.architecture.FrameworkRepo
import org.mozilla.focus.ext.LiveDataCombiners
import org.mozilla.focus.session.SessionRepo

/**
 * The view state, and UI event callback interface, of the browser UI component.
 */
class BrowserViewModel(
    frameworkRepo: FrameworkRepo,
    private val sessionRepo: SessionRepo
) : ViewModel() {

    private val isNavigationOverlayVisible = MutableLiveData<Boolean>()

    val isWebViewVisible = LiveDataCombiners.combineLatest(
        frameworkRepo.isVoiceViewEnabled,
        isNavigationOverlayVisible
    ) { isVoiceViewEnabled, isNavigationOverlayVisible ->
        // We want to disable accessibility on the WebView when the nav overlay is visible so users
        // cannot focus the WebView content below the overlay. Unfortunately, isFocusable* and
        // setImportantForAccessibility didn't work so the only way I could disable WebView accessibility
        // was to hide it. This diverges the screen reader and visual experience, a bad practice.
        val isWebViewHidden = isVoiceViewEnabled && isNavigationOverlayVisible
        !isWebViewHidden
    }

    // TODO: update property reactively from model
    @UiThread
    fun onNavigationOverlayVisibilityChange(isVisible: Boolean) {
        isNavigationOverlayVisible.value = isVisible
    }

    fun fullscreenChanged(isFullscreen: Boolean) {
        sessionRepo.fullscreenChange(isFullscreen)
    }
}

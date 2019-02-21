/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.focus.architecture.FrameworkRepo
import org.mozilla.focus.ext.LiveDataCombiners
import org.mozilla.focus.ext.switchMap
import org.mozilla.focus.session.SessionRepo
import kotlin.coroutines.CoroutineContext

/**
 * The view state, and UI event callback interface, of the browser UI component.
 */
class BrowserViewModel(
    frameworkRepo: FrameworkRepo,
    private val sessionRepo: SessionRepo,
    private val coroutineContext: CoroutineContext = Main // LiveData eventually runs on main so just start there.
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

    // Draw a background behind fullscreen views - this serves the following purposes:
    // - Be a background: the fullscreen content may not fill the screen so this background will be seen behind it
    // - Prevent flickering when exiting fullscreen (see below)
    //
    // There may be flickering between the toolbar and the fullscreen video when exiting fullscreen:
    // my hypothesis is that there is a gap between these views where no view is drawn, showing the user
    // an invalid graphics buffer (which are known to have visual artifacts). We fill the gap with the window
    // background because using views inside the view hierarchy didn't seem to work; using the window background
    // is also a simpler solution.
    //
    // The whole screen is usually covered by the WebView so if left the window background on all the time,
    // we would have an extra layer of overdraw and performance suffers: enabling the window background always,
    // using the Profile GPU rendering option, and interacting with the application, frames are noticeably dropped.
    // More details on windowBackground and overdraw:
    //   https://android-developers.googleblog.com/2009/03/window-backgrounds-ui-speed.html
    private var windowBackgroundDeferredUpdateJob: Job? = null
    private var _isWindowBackgroundEnabled = MutableLiveData<Boolean>()
    val isWindowBackgroundEnabled: LiveData<Boolean> = sessionRepo.isFullscreen.switchMap { newValue ->
        fun delay(millis: Long, action: () -> Unit) = GlobalScope.launch(coroutineContext) {
            delay(millis)
            action()
        }

        // Cancel the deferred job: if we need it, we'll reschedule it. This simplifies the state update because we
        // don't need to handle emissions when there is an active job and when there is no active job.
        windowBackgroundDeferredUpdateJob?.cancel()
        windowBackgroundDeferredUpdateJob = null

        val lastEmittedValue = _isWindowBackgroundEnabled.value
        val isExitingFullscreen = lastEmittedValue == true && !newValue

        // We return the backing LiveData using switchMap so we have a LiveData reference to update from our deferred job.
        _isWindowBackgroundEnabled.also {
            if (isExitingFullscreen) {
                windowBackgroundDeferredUpdateJob = delay(millis = 5000) { it.value = false }
            } else {
                it.value = newValue
            }
        }
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

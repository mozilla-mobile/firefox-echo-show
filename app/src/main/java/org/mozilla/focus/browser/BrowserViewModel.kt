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
    // an invalid graphics buffer (which are known to have visual artifacts). Setting the background color of the
    // fullscreen container does not fix the issue so we fill the gap with another view's background. This space is
    // usually covered by the WebView or the fullscreen container so to prevent overdraw and improve performance, we
    // dynamically show and hide the background view.
    private var fullscreenBackgroundDeferredUpdateJob: Job? = null
    private var _isFullscreenBackgroundEnabled = MutableLiveData<Boolean>()
    val isFullscreenBackgroundEnabled: LiveData<Boolean> = sessionRepo.isFullscreen.switchMap { newValue ->
        fun delay(millis: Long, action: () -> Unit) = GlobalScope.launch(coroutineContext) {
            delay(millis)
            action()
        }

        // Cancel the deferred job: if we need it, we'll reschedule it. This simplifies the state update because we
        // don't need to handle emissions when there is an active job and when there is no active job.
        fullscreenBackgroundDeferredUpdateJob?.cancel()
        fullscreenBackgroundDeferredUpdateJob = null

        val lastEmittedValue = _isFullscreenBackgroundEnabled.value
        val isExitingFullscreen = lastEmittedValue == true && !newValue

        // We return the backing LiveData using switchMap so we have a LiveData reference to update from our deferred job.
        _isFullscreenBackgroundEnabled.also {
            if (isExitingFullscreen) {
                // The background needs to remain visible during the animation to exit fullscreen mode. Unfortunately,
                // we get an emission when the animation begins so we wait a short duration before disabling the background.
                fullscreenBackgroundDeferredUpdateJob = delay(millis = 5000) { it.value = false }
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

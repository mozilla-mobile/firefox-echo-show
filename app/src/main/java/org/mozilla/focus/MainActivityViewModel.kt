/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.focus.session.SessionRepo

/**
 * A view model representing UI controlled by the [MainActivity].
 */
class MainActivityViewModel(
    sessionRepo: SessionRepo
) : ViewModel() {

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
    // we would have an extra layer of overdraw and performance may suffer. Since these devices are low powered,
    // it seems worthwhile to preemptively optimize this. More details on windowBackground and overdraw:
    //   https://android-developers.googleblog.com/2009/03/window-backgrounds-ui-speed.html
    val isWindowBackgroundEnabled: LiveData<Boolean> = sessionRepo.isFullscreen
}

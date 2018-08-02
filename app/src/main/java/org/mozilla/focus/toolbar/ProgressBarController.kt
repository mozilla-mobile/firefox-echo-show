/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

/** Controls the appearance of the progress bar. */
class ProgressBarController(
        private val progressBar: UrlBoxProgressView
) {

    fun onProgressUpdate(rawProgressValue: Int) {
        // The max progress value is 99 (see comment in onProgress() in SessionCallbackProxy),
        // thus we send 100 to the UrlBoxProgressView to complete its animation.
        val adjustedProgressValue = if (rawProgressValue == 99) 100 else rawProgressValue
        progressBar.progress = adjustedProgressValue

        // If the progress is 100% then we want to go back to 0 to hide the progress drawable
        // again. However we want to show the full progress bar briefly so we wait 250ms before
        // going back to 0.
        if (adjustedProgressValue == 100) {
            launch(UI) {
                delay(250)
                progressBar.progress = 0
            }
        }
    }
}

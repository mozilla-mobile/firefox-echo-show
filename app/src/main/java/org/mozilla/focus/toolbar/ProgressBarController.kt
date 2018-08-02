/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

/** Controls the appearance of the progress bar. */
class ProgressBarController(
        private val progressBar: UrlBoxProgressView
) {

    fun onProgressUpdate(rawProgressValue: Int) {
        // The max progress value is 99 (see comment in onProgress() in SessionCallbackProxy),
        // thus we send 100 to the UrlBoxProgressView to complete its animation.
        val adjustedProgressValue = if (rawProgressValue == 99) 100 else rawProgressValue
        progressBar.progress = adjustedProgressValue
    }
}

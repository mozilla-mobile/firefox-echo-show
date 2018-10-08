/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

private const val MIN_UI_PROGRESS = 0
private const val MAX_UI_PROGRESS = 100

/** Controls the appearance of the progress bar. */
class ProgressBarController(
    private val progressBar: UrlBoxBackgroundWithProgress
) {
    private var isLoading = false
    private var hideProgressBarJob: Job? = null

    fun onLoadingUpdate(isLoading: Boolean) {
        this.isLoading = isLoading
        if (isLoading) {
            cancelPendingHideProgressBarJob()
        } else {
            hideProgressBar()
        }
    }

    /**
     * @param progressValue A progress value between 0 and 99.
     */
    fun onProgressUpdate(progressValue: Int) {
        // On some pages, like cnn articles, this function may get called after the progress bar
        // completes for the first time, causing the progress bar to look like it's loading again
        // or blink on and off. To avoid this, we early return if we're not in the loading state.
        if (!isLoading) {
            return
        }

        progressBar.progress = progressValue
    }

    private fun cancelPendingHideProgressBarJob() {
        hideProgressBarJob?.let {
            it.cancel()
            hideProgressBarJob = null
        }
    }

    private fun hideProgressBar() {
        if (progressBar.progress == MIN_UI_PROGRESS || // Already hidden.
                hideProgressBarJob != null) { // Already hiding.
            return
        }

        // The progress bar may hide even when set to an arbitrary percentage and it looks weird:
        // we max out the progress bar briefly to make it look more natural.
        progressBar.progress = MAX_UI_PROGRESS
        hideProgressBarJob = launch(UI, CoroutineStart.UNDISPATCHED) {
            delay(250)
            progressBar.progress = MIN_UI_PROGRESS
        }
    }
}

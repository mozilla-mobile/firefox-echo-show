/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import mozilla.components.support.utils.SafeIntent
import org.mozilla.focus.ext.getUriToOpen
import org.mozilla.focus.session.SessionRestorer

/**
 * Takes [Intent]s sent to [MainActivity] and tells it what to do in response to them. This class is intended to
 * free [MainActivity] from having to handle Intents.
 */
class MainActivityIntentResponder constructor(
    private val loadUrl: (String) -> Unit
) {

    fun onCreate(context: Context, savedInstanceState: Bundle?, sessionRestorer: SessionRestorer, intent: SafeIntent) {
        // This Intent was launched from history (recent apps): this may not be called on Echo Show but
        // handling it may be useful for testing on the Android emulator.
        //
        // This code may be unnecessary: it's from Focus and I do not know how to reproduce this code path
        // in manual testing.
        if (intent.isLaunchedFromHistory) {
            return
        }

        // If we're restoring, a redelivered Intent will be received in this method and onNewIntent will be called
        // with the new Intent.
        val isActivityRestoring = savedInstanceState != null
        if (isActivityRestoring) {
            // If the Activity is restoring, we want to restore our session from memory. This is handled by
            // IWebViewLifecycleFragment: ideally, that code would be centralized here. However, that refactor
            // should happen after we move to components. As such, we return assuming the fragment will handle it.
            return
        }

        // Ideally, if there was both a URL to restore and an Intent URI to open, we would add both to the back stack.
        // However, issue #254 means we would create a new session for each url (i.e. 2 total) and the first loaded url
        // would be inaccessible so we just load the one the user probably intends.
        //
        // In future iterations, we should consider loading these URLs reactively.
        val urlToLoad = intent.getUriToOpen(context) ?: sessionRestorer.getPersistedSessionUrl()
        urlToLoad?.let(loadUrl)
    }

    fun onNewIntent(context: Context, intent: SafeIntent) {
        intent.getUriToOpen(context)?.let(loadUrl)
    }
}

private val SafeIntent.isLaunchedFromHistory get() = flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

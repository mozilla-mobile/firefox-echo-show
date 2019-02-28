/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.os.Bundle

/**
 * Takes [Intent]s sent to [MainActivity] and tells it what to do in response to them. This class is intended to
 * free [MainActivity] from having to handle Intents.
 */
class MainActivityIntentResponder(
    private val loadUrl: (String) -> Unit
) {

    fun onCreate(context: Context, savedInstanceState: Bundle?, intentValidator: IntentValidator) {
        intentValidator.getUriToOpen(context, savedInstanceState)?.let(loadUrl)
    }

    fun onNewIntent(context: Context, intentValidator: IntentValidator) {
        intentValidator.getUriToOpen(context)?.let(loadUrl)
    }
}

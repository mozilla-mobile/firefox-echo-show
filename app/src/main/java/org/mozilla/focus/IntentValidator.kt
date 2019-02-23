/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import mozilla.components.support.utils.SafeIntent
import org.mozilla.focus.utils.UrlUtils

/**
 * An encapsulation of functions that extract values from valid Intents.
 *
 * The class constructor takes a [SafeIntent] to encourage the caller to use SafeIntents
 * in their code.
 */
class IntentValidator(
    private val intent: SafeIntent
) {

    /**
     * Takes the uri defined in the [SafeIntent] and converts it into the uri the app should open.
     *
     * @return the uri for the app to open or null if there is none.
     */
    fun getUriToOpen(context: Context, savedInstanceState: Bundle? = null): String? {
        fun isIntentRedelivered(): Boolean {
            // This Intent was launched from history (recent apps): this may not be called on Echo Show but
            // handling it may be useful for testing on the Android emulator.
            //
            // I do not know how to reproduce this code path in manual testing.
            return (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) ||

                // The Activity is being restored.
                //
                // You can reproduce this code path manually on an Android emulator by enabling don't keep
                // activities, opening FF, clicking home, opening the recent apps, and opening FF from there.
                savedInstanceState != null
        }

        // N.B. the code path for restoring state is hard to understand because this Intent handling code should
        // be tightly integrated with Android's Activity restoration and our EngineView session restore but it's not.
        // We could spend time improving this but we should wait to integrate with components first. As such, this
        // code is similar to how it was when we forked Focus and may do unnecessary things.
        //
        // If an intent is being redelivered, we already have some state and our session restore process will handle
        // restoring it so the redundant Intent is not helpful.
        if (isIntentRedelivered()) return null

        return when (intent.action) {
            // ACTION_VIEW is only sent internally: we want the preinstalled WebView
            // application to handle system-wide HTTP(S) intents.
            ACTION_VIEW -> intent.dataString?.ifBlank { null }
            ACTION_SEND -> {
                val dataString = intent.getStringExtra(Intent.EXTRA_TEXT)?.ifBlank { null }
                dataString?.let {
                    val isSearch = !UrlUtils.isUrl(dataString)
                    if (isSearch) UrlUtils.createSearchUrl(context, dataString) else dataString
                }
            }

            else -> null
        }
    }
}

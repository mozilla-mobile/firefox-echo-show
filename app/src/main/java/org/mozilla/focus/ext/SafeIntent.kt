/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import mozilla.components.support.utils.SafeIntent
import org.mozilla.focus.utils.UrlUtils

/**
 * Takes the uri defined in the [SafeIntent] and converts it into the uri the app should open.
 *
 * @return the uri for the app to open or null if there is none.
 */
fun SafeIntent.getUriToOpen(context: Context): String? {
    return when (action) {
        // ACTION_VIEW is only sent internally: we want the preinstalled WebView
        // application to handle system-wide HTTP(S) intents.
        ACTION_VIEW -> dataString?.ifBlank { null }
        ACTION_SEND -> {
            val dataString = getStringExtra(Intent.EXTRA_TEXT)?.ifBlank { null }
            dataString?.let {
                val isSearch = !UrlUtils.isUrl(dataString)
                if (isSearch) UrlUtils.createSearchUrl(context, dataString) else dataString
            }
        }

        else -> null
    }
}

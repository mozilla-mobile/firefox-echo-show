/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.mozilla.focus.MainActivity

/**
 * Encapsulates internal interactions with the browser.
 */
object Browser {

    /** Gets an Intent to open the browser. */
    fun getIntent(context: Context, dataUri: Uri) = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = dataUri
    }
}

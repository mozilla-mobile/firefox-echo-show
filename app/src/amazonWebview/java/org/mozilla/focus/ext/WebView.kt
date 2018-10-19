/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.webkit.WebView
import org.mozilla.focus.iwebview.WebViewProvider

fun WebView.deleteData() {
    clearFormData()
    clearHistory()
    clearMatches()
    clearSslPreferences()
    clearCache(true)

    WebViewProvider.deleteGlobalData(context)
}

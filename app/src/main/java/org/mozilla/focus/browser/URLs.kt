/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.net.Uri
import org.mozilla.focus.ext.toUri

/**
 * A collection of known URLs and parts of URLs.
 */
object URLs {
    // We can't use "about:" because webview silently swallows about: pages,
    // hence we use a custom scheme.
    private const val APP_URL_PREFIX = "firefox"
    val APP_URL_STARTUP_HOME = "$APP_URL_PREFIX:home".toUri() as Uri
    @JvmField val URL_ABOUT = "$APP_URL_PREFIX:about".toUri() as Uri

    // We can load android assets directly.
    const val URL_ABOUT_LICENSES = "file:///android_asset/licenses.html"
    const val URL_ABOUT_GPL = "file:///android_asset/gpl.html"

    val URL_PRIVACY_NOTICE = "https://www.mozilla.org/privacy/firefox-fire-tv/".toUri() as Uri
}

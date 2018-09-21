/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.ContentResolver
import android.net.Uri
import org.mozilla.focus.ext.toUri

/**
 * A collection of known URLs and parts of URLs.
 */
object URLs {
    // We can't use "about:" because webview silently swallows about: pages,
    // hence we use a custom scheme.
    private const val SCHEME_APP = "firefox"
    private const val SCHEME_FILE = ContentResolver.SCHEME_FILE

    private const val PATH_ANDROID_ASSET = "/android_asset"

    val APP_STARTUP_HOME = "$SCHEME_APP:home".toUri() as Uri
    @JvmField val APP_ABOUT = "$SCHEME_APP:about".toUri() as Uri

    val ASSET_ABOUT = "$SCHEME_FILE://$PATH_ANDROID_ASSET/about.html".toUri() as Uri

    @JvmField val ABOUT_BLANK = "about:blank".toUri() as Uri

    // We can load android assets directly.
    const val URL_ABOUT_LICENSES = "file:///android_asset/licenses.html"
    const val URL_ABOUT_GPL = "file:///android_asset/gpl.html"

    val PRIVACY_NOTICE = "https://www.mozilla.org/privacy/firefox-fire-tv/".toUri() as Uri
}

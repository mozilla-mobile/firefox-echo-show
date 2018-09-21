/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.net.Uri
import org.mozilla.focus.browser.URLs
import org.mozilla.focus.utils.Either

internal typealias HintText = Unit

/**
 * Manipulations of toolbar text.
 */
object ToolbarText {

    /**
     * Gets the text that should be displayed in the toolbar for a given uri.
     *
     * @return either the updated toolbar text or an indication that a hint should be used instead.
     */
    fun getDisplayText(uri: Uri): Either<String, HintText> {
        if (uri == URLs.APP_STARTUP_HOME) {
            return Either.Right(HintText)
        }

        return (if (uri.scheme == URLs.SCHEME_FILE && uri.path.startsWith(URLs.PATH_ANDROID_ASSET)) {
            val uriNewScheme = uri.buildUpon().scheme(URLs.SCHEME_APP).build()

            // Currently: `app:///android_asset/...` Unfortunately, there is no way to remove the
            // "//" following the scheme using Uri.Builder so we modify the String by hand.
            uriNewScheme.toString()
                    .replace("//${URLs.PATH_ANDROID_ASSET}/", "")
                    .replace(".html", "")
        } else {
            uri.toString()
        }).let { Either.Left(it) }
    }
}

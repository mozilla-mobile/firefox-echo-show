/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.net.Uri
import org.mozilla.focus.ext.isYouTubeDesktop
import org.mozilla.focus.iwebview.IWebView

/**
 * Encapsulates functionality to address web compat issues.
 */
object WebCompat {

    fun onSessionLoadingChanged(isLoading: Boolean, uri: Uri, webView: IWebView) {
        unfocusSearchBoxOnYoutube(isLoading, uri, webView)
    }

    private fun unfocusSearchBoxOnYoutube(isLoading: Boolean, uri: Uri, webView: IWebView) {
        // If the search box is focused while the page starts to load, the soft keyboard will
        // display again around when the page finishes loading. To avoid this issue, we unfocus
        // the search box when page load starts (#198).
        //
        // It's not clear why this happens. The best theory is that when the page content is
        // mutated, focus moves away from the search box and back to the search box. It's unclear
        // why focus moves back to the search box when the search box was focused but does not move
        // to the search box when the search box is not focused. Perhaps:
        // - They're replacing those components in the DOM, which would unfocus them. Maybe they, or
        // the browser, puts focus back on the component that was focused before.
        // - The click listener that starts the page load (e.g. video thumbnail) takes focus but
        // returns it to the element that had focus before itself
        if (isLoading && uri.isYouTubeDesktop) {
            webView.evalJS("document.querySelector('ytd-searchbox input').blur();")
        }
    }
}

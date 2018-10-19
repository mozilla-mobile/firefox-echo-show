/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec
import java.io.ByteArrayOutputStream

private const val EXPECTED_WEBVIEW_COUNT = 1 // This will be 0 if we add the WebView dynamically.
private const val WEBVIEW_CLASS_NAME = "org.mozilla.focus.iwebview.IWebView"

// We use shell commands to traverse over the files because it's a trivial amount of code
// compared to writing it ourselves and it's probably more optimized than we could write.
private const val WEBVIEW_COUNT_CMD = """find src -name "*.xml" \
    -exec grep -o $WEBVIEW_CLASS_NAME {} + | wc -l"""

/**
 * Verifies the number of WebViews present in the XML resources.
 *
 * This is necessary because the Echo Show will kill the application if there is more
 * than one WebView instance in the app (#433, #540). It is extremely difficult to
 * ensure only a single WebView instance is created (e.g. they can be added dynamically)
 * but this task makes a partial effort via static analysis.
 */
open class VerifyWebViewCount : Exec() {

    private val stdOutCapture = ByteArrayOutputStream()

    init {
        group = "Verification"
        description = "Verifies there are at most $EXPECTED_WEBVIEW_COUNT '$WEBVIEW_CLASS_NAME' instances in XML"

        commandLine = listOf("sh", "-c", WEBVIEW_COUNT_CMD)
        standardOutput = stdOutCapture
        this.isIgnoreExitValue = true // We'll parse stdout to determine task result.

        this.doLast {
            val iWebViewCount = stdOutCapture.toByteArray().inputStream().bufferedReader().use {
                it.readText()
            }.trim().toIntOrNull() ?: throw GradleException("Return value of command is unexpectedly not a number")

            if (iWebViewCount != EXPECTED_WEBVIEW_COUNT) {
                throw GradleException("Expected at most $EXPECTED_WEBVIEW_COUNT '$WEBVIEW_CLASS_NAME' in XML: " +
                        "found $iWebViewCount. See ${this::class.java.simpleName} kdoc for more details.")
            }
        }
    }
}

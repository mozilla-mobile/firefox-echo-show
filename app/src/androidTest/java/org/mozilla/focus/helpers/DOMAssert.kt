/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helpers

import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import org.hamcrest.CoreMatchers.equalTo

/**
 * A collection of assertions for the DOM.
 */
object DOMAssert {

    fun assertBodyText(expected: String) {
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "body"))
                .check(webMatches(getText(), equalTo(expected)))
    }
}

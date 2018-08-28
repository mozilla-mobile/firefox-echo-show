/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helpers

import android.support.test.espresso.web.assertion.WebViewAssertions.webMatches
import android.support.test.espresso.web.sugar.Web.onWebView
import android.support.test.espresso.web.webdriver.DriverAtoms.findElement
import android.support.test.espresso.web.webdriver.DriverAtoms.getText
import android.support.test.espresso.web.webdriver.Locator
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

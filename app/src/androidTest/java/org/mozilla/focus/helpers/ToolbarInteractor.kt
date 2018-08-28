/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helpers

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withHint
import org.hamcrest.Matchers.allOf
import org.mozilla.focus.R

/**
 * Encapsulations of interactions with the toolbar.
 */
object ToolbarInteractor {

    /**
     * Enters and submits a URL.
     *
     * NB: this doesn't wait for the page to finish loading.
     */
    fun enterAndSubmitURL(url: String) {
        onView(allOf(withHint(R.string.urlbar_hint), isDisplayed())) // Display mode.
                .perform(click())
        onView(allOf(withHint(R.string.urlbar_hint), isDisplayed())) // Edit mode.
                .perform(typeText(url), pressImeActionButton())
    }
}

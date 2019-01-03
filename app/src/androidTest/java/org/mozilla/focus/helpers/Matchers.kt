/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helpers

import android.support.test.espresso.matcher.BoundedMatcher
import android.support.v7.widget.RecyclerView
import android.view.View
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.Matcher
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed as espressoIsDisplayed

// TODO: replace with components implementation: #155.

/**
 * The [espressoIsDisplayed] function that can also handle unchecked state through the boolean argument.
 */
fun isDisplayed(isDisplayed: Boolean): Matcher<View> = maybeInvertMatcher(espressoIsDisplayed(), isDisplayed)

private fun maybeInvertMatcher(matcher: Matcher<View>, useUnmodifiedMatcher: Boolean): Matcher<View> = when {
    useUnmodifiedMatcher -> matcher
    else -> not(matcher)
}

/**
 * Asserts the RecyclerView has the given item count.
 *
 * via https://stackoverflow.com/a/50130818
 */
fun hasItemCount(count: Int): Matcher<View> = object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
    override fun describeTo(description: Description?) {
        description?.appendText("has $count items")
    }

    override fun matchesSafely(view: RecyclerView?): Boolean = view?.adapter?.itemCount == count
}

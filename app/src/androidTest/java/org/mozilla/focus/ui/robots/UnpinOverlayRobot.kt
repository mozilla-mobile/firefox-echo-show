/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ui.robots

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.mozilla.focus.R
import org.mozilla.focus.helpers.isDisplayed

/**
 * Implementation of Robot Pattern for the home tile unpin overlay.
 */
class UnpinOverlayRobot private constructor() {

    private fun assertIsDisplayed(isDisplayed: Boolean = true) {
        unpinButton().check(matches(isDisplayed(isDisplayed)))
    }

    class Transition {

        fun unpinTileToNavigationOverlay(interact: NavigationOverlayRobot.() -> Unit): NavigationOverlayRobot.Transition {
            unpinButton().perform(click())
            return NavigationOverlayRobot.interactAndTransition(interact)
        }

        @Suppress("UNUSED_PARAMETER") // TODO: remove when fixing this method.
        fun dismissToNavigationOverlay(interact: NavigationOverlayRobot.() -> Unit): NavigationOverlayRobot.Transition {
            throw NotImplementedError("TODO: fix this method. the simple implementation clicks the center of the " +
                "overlay, i.e. the remove button, and does not dismiss without removing")
//            semiOpaqueOverlay().perform(click())
//            return NavigationOverlayRobot.interactAndTransition(interact)
        }
    }

    companion object {
        fun interactAndTransition(interact: UnpinOverlayRobot.() -> Unit): UnpinOverlayRobot.Transition {
            UnpinOverlayRobot().run {
                assertIsDisplayed()
                interact()
            }
            return UnpinOverlayRobot.Transition()
        }
    }
}

private fun unpinButton() = onView(withId(R.id.unpinButton))
private fun semiOpaqueOverlay() = onView(withId(R.id.unpinOverlay))

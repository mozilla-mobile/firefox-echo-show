/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.MainActivity;
import org.mozilla.focus.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
@Ignore // TODO: fix me! #525
public class PinnedTileTests {

    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class);

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void testCustomPinnedTile() throws InterruptedException, UiObjectNotFoundException {
//        onView(allOf(withId(R.id.urlInputView), isDisplayed(), hasFocus()))
//                .perform(typeTextIntoFocusedView("example.com"))
//                .perform(pressImeActionButton());

        onView(ViewMatchers.withId(R.id.webview))
                .check(matches(isDisplayed()));

        mDevice.pressMenu();

//        final ViewInteraction pinButton = onView(ViewMatchers.withId(R.id.pinButton))
//                .check(matches(isNotChecked()));

//        pinButton.perform(click());

        onView(withText(R.string.notification_pinned_site))
                .inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));

//        final ViewInteraction goHome = onView(ViewMatchers.withId(R.id.navButtonHome))
//                .check(matches(isDisplayed()));

//        goHome.perform(click());

        // UIAutomator work-around waiting for tile existence
        UiObject newTile = mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.tv.firefox.debug:id/tile_title")
                .text("example")
                .enabled(true));
        newTile.waitForExists(5000);

        newTile.click();

        mDevice.pressMenu();

//        pinButton.perform(click());

        onView(withText(R.string.notification_unpinned_site))
                .inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));

//        goHome.perform(click());

        // UIAutomator work-around waiting for tile non-existence
        newTile.waitUntilGone(5000);
        assertFalse(newTile.exists());
    }
}

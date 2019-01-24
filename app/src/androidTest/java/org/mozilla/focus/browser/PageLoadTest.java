/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.MainActivity;
import org.mozilla.focus.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;

@RunWith(AndroidJUnit4.class)
@Ignore // TODO: fix me! #525
public class PageLoadTest {

    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private static final Integer TILE_POSITION = 1; // Google Video Search
    private static final String TILE_WEBSITE_ELEMENT = "hplogo"; // Google logo
    private static final String MOZILLA_URL = "mozilla.org";
    private static final String MOZILLA_PAGE_ELEMENT = ".content h2";

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class);

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void PageLoadTest() throws InterruptedException, UiObjectNotFoundException {
//        onView(ViewMatchers.withId(R.id.tileContainer))
//                .perform(RecyclerViewActions.actionOnItemAtPosition(TILE_POSITION, click()));

        onView(ViewMatchers.withId(R.id.webview))
                .check(matches(isDisplayed()));

        onWebView()
                .withElement(findElement(Locator.ID, TILE_WEBSITE_ELEMENT));

        mDevice.pressMenu();

//        onView(ViewMatchers.withId(R.id.navUrlInput))
//                .check(matches(isDisplayed()))
//                .check(matches(withText(containsString("google"))));

//        onView(ViewMatchers.withId(R.id.navButtonHome))
//                .check(matches(isDisplayed()))
//                .perform(click());

//        onView(allOf(withId(R.id.urlInputView), isDisplayed(), hasFocus()))
//                .perform(typeTextIntoFocusedView(MOZILLA_URL))
//                .perform(pressImeActionButton());

        onView(ViewMatchers.withId(R.id.webview))
                .check(matches(isDisplayed()));

        onWebView()
                .withElement(findElement(Locator.CSS_SELECTOR, MOZILLA_PAGE_ELEMENT));

        mDevice.pressMenu();

//        onView(ViewMatchers.withId(R.id.navUrlInput))
//                .check(matches(isDisplayed()))
//                .check(matches(withText(containsString("mozilla"))));
    }
}

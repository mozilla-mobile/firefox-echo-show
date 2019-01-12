/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import mozilla.components.browser.toolbar.BrowserToolbar
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserAppBarLayoutControllerTest {

    private lateinit var controller: BrowserAppBarLayoutController

    private lateinit var context: Context
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: BrowserToolbar

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        appBarLayout = mock(AppBarLayout::class.java).also {
            `when`(it.context).thenReturn(context)
        }

        toolbar = mock(BrowserToolbar::class.java).also {
            // Some methods assume layoutParams will be non-null.
            `when`(it.layoutParams).thenReturn(mock(AppBarLayout.LayoutParams::class.java))
        }

        controller = BrowserAppBarLayoutController(mock(BrowserAppBarViewModel::class.java), appBarLayout, toolbar)
    }

    @Test
    fun `WHEN the AppBarLayout controller is used THEN touch exploration state listeners are added and removed`() {
        val a11yManager = mock(AccessibilityManager::class.java)
        `when`(context.getSystemService(Context.ACCESSIBILITY_SERVICE)).thenReturn(a11yManager)

        fun verifyTouchExplorationStateChangeListenerCalled(addTimes: Int, removeTimes: Int) {
            verify(a11yManager, times(addTimes)).addTouchExplorationStateChangeListener(any(
                TouchExplorationStateChangeListener::class.java))
            verify(a11yManager, times(removeTimes)).removeTouchExplorationStateChangeListener(any(
                TouchExplorationStateChangeListener::class.java))
        }

        verifyTouchExplorationStateChangeListenerCalled(addTimes = 0, removeTimes = 0)
        controller.onStart()
        verifyTouchExplorationStateChangeListenerCalled(addTimes = 1, removeTimes = 0)
        controller.onStop()
        verifyTouchExplorationStateChangeListenerCalled(addTimes = 1, removeTimes = 1)
    }
}

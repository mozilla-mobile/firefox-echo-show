/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Executes [doOnSwipeDown] when down swipes are detected.
 *
 * TODO this was written to be used in #106, but related functionality was
 * split off into #204 pending UX input.  After these decisions have been made,
 * this class should be either used or deleted
 */
class SwipeDownListener(private val doOnSwipeDown: () -> Unit) : GestureDetector.SimpleOnGestureListener() {

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null || e2 == null) return false
        val startX = e1.rawX
        val startY = e1.rawY
        val endX = e2.rawX
        val endY = e2.rawY
        val diffX = endX - startX
        val diffY = endY - startY

        val swipeIsVertical = abs(diffY) > abs((diffX))
        if (!swipeIsVertical) return false

        val swipeIsDown = diffY > 0

        if (swipeIsDown) {
            doOnSwipeDown.invoke()
            return true
        }
        return false
    }
}

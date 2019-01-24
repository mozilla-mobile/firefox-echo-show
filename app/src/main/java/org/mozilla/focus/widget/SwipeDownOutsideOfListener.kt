/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import org.mozilla.focus.ext.isWithinBoundsOf
import org.mozilla.focus.utils.WeakReferenceDelegate
import kotlin.math.abs

/**
 * Calls [doOnSwipeDown] any time a swipe down gesture occurs, unless that
 * gesture started within the view bounds of [invalidTarget]
 */
class SwipeDownOutsideOfListener : GestureDetector.SimpleOnGestureListener() {

    private var invalidTarget: View? by WeakReferenceDelegate()
    private var doOnSwipeDown: (() -> Unit)? = null

    fun disable() {
        invalidTarget = null
        doOnSwipeDown = null
    }

    fun enable(invalidTarget: View, doOnSwipeDown: () -> Unit) {
        this.invalidTarget = invalidTarget
        this.doOnSwipeDown = doOnSwipeDown
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        val invalidTarget = invalidTarget
        val doOnSwipeDown = doOnSwipeDown
        if (e1 == null || e2 == null || invalidTarget == null || doOnSwipeDown == null) return false
        if (e1 isWithinBoundsOf invalidTarget) return false
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

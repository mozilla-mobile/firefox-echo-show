/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.view.MotionEvent
import android.view.View
import kotlin.math.round

/** Calls the functionBlock and then [MotionEvent.recycle]s this. Inspired by [AutoCloseable.use]. */
inline fun <R> MotionEvent.use(functionBlock: (MotionEvent) -> R): R {
    try {
        return functionBlock(this)
    } finally {
        recycle()
    }
}

infix fun MotionEvent?.isWithinBoundsOf(view: View?): Boolean {
    view ?: return false
    this ?: return false
    val x = round(this.x).toInt()
    val y = round(this.y).toInt()
    return x in view.left..view.right &&
            y in view.top..view.bottom
}

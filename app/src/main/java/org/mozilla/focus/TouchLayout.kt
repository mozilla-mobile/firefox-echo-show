/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.lang.ref.WeakReference
import kotlin.math.round

/**
 * Allows us to intercept touch events happening anywhere on the screen without
 * consuming them.
 */
class TouchInterceptorLayout(
    context: Context,
    attrs: AttributeSet?
) : CoordinatorLayout(context, attrs) {

    private val touchOutsideListeners = mutableListOf<Pair<WeakReference<View>, () -> Unit>>()

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            touchOutsideListeners.filter { (viewRef, _) -> !(event isWithinBoundsOf viewRef.get()) }
                    .forEach { (_, action) -> action.invoke() }
        }

        return super.onInterceptTouchEvent(event)
    }

    /**
     * Registers an action to be fired when a touch event occurs that is outside
     * of the bounds of the passed view. Pass a null action to remove a listener
     */
    fun setOnTouchOutsideViewListener(view: View, action: (() -> Unit)?) {
        if (action == null) {
            touchOutsideListeners.find { it.first.get() == view }
                    ?.let { touchOutsideListeners.remove(it) }
        } else {
            touchOutsideListeners += WeakReference(view) to action
        }
    }
}

private infix fun MotionEvent?.isWithinBoundsOf(view: View?): Boolean {
    view ?: return false
    this ?: return false
    val x = round(this.x).toInt()
    val y = round(this.y).toInt()
    return x in view.left..view.right &&
            y in view.top..view.bottom
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/**
 * A [FrameLayout] that has an [onInterceptTouchEvent] equivalent to [View.setOnTouchListener].
 */
class OnInterceptTouchEventFrameLayout(
    context: Context,
    attrs: AttributeSet
) : FrameLayout(context, attrs) {
    var onInterceptTouchEventObserver: ((MotionEvent?) -> Unit)? = null

    override fun onInterceptTouchEvent(motionEvent: MotionEvent?): Boolean {
        motionEvent?.let { onInterceptTouchEventObserver?.invoke(it) }
        return super.onInterceptTouchEvent(motionEvent)
    }
}

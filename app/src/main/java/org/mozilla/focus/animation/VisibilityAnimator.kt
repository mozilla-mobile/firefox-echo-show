/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.animation

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

/**
 * Provides a simple alpha animation between Visible and Gone that is usable by
 * any View
 *
 * #### This duplicates Transition functionality, and should not be expanded before
 * approaches are compared
 */
object VisibilityAnimator {
    fun animateVisibility(view: View, visible: Boolean) {
        val (fromAlpha, toAlpha, toVisibility) = when (visible) {
            true -> Triple(0f, 1f, View.VISIBLE)
            false -> Triple(1f, 0f, View.GONE)
        }
        if (view.visibility == toVisibility) return
        val animation = AlphaAnimation(fromAlpha, toAlpha).apply {
            duration = 150
            fillAfter = false
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationStart(animation: Animation?) {
                    view.visibility = View.VISIBLE
                }
                override fun onAnimationEnd(animation: Animation?) {
                    view.visibility = toVisibility
                }
            })
        }
        view.startAnimation(animation)
    }
}

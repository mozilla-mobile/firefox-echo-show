/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.view.View
import kotlinx.android.synthetic.main.fragment_navigation_overlay.view.*
import org.mozilla.focus.ext.onGlobalLayoutOnce

/**
 * Encapsulation of animation code for the navigation overlay.
 */
object NavigationOverlayAnimations {

    fun onCreateViewAnimateIn(overlay: View, isInitialHomescreen: Boolean, isBeingRestored: Boolean, onAnimationEnd: () -> Unit) {
        if (isInitialHomescreen || isBeingRestored) {
            onAnimationEnd()
            return
        }

        // View positions are not set in onCreateView so we must wait for layout.
        overlay.onGlobalLayoutOnce {
            getAnimator(overlay, isAnimateIn = true, onAnimationEnd = onAnimationEnd).start()
        }
    }

    fun animateOut(overlay: View, isInitialHomescreen: Boolean, onAnimationEnd: () -> Unit) {
        getAnimator(overlay, isAnimateIn = false, isInitialHomescreen = isInitialHomescreen, onAnimationEnd = onAnimationEnd)
            .start()
    }

    @Suppress("SpreadOperator") // It reduces repetition and the arrays are small so perf impact is negligible.
    private fun getAnimator(
        overlay: View,
        isAnimateIn: Boolean,
        isInitialHomescreen: Boolean = false,
        onAnimationEnd: () -> Unit
    ): Animator {
        val fadeValues = if (isAnimateIn) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f)
        val semiOpaqueBackgroundAnimator = ObjectAnimator.ofFloat(overlay.semiOpaqueBackground, "alpha", *fadeValues)

        val initialHomescreenBackgroundAnimator = if (!isInitialHomescreen) {
            null
        } else {
            ObjectAnimator.ofFloat(overlay.initialHomescreenBackground, "alpha", 1f, 0f)
        }

        val screenHeight = overlay.resources.displayMetrics.heightPixels.toFloat()
        fun getTranslateUpAnimator(view: View): Animator {
            val viewOffsetToBottomOfScreen = screenHeight - view.y
            val translateValues =
                if (isAnimateIn) floatArrayOf(viewOffsetToBottomOfScreen, 0f) else floatArrayOf(0f, viewOffsetToBottomOfScreen)
            return ObjectAnimator.ofFloat(view, "translationY", *translateValues)
        }

        return AnimatorSet().apply {
            duration = 400
            interpolator = FastOutSlowInInterpolator()

            val animatorSetBuilder = play(semiOpaqueBackgroundAnimator)
            arrayOf(overlay.homeTiles, overlay.backgroundView, overlay.backgroundShadowView).forEach {
                animatorSetBuilder.with(getTranslateUpAnimator(it))
            }
            initialHomescreenBackgroundAnimator?.let { animatorSetBuilder.with(it) }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    onAnimationEnd()
                }
            })
        }
    }
}

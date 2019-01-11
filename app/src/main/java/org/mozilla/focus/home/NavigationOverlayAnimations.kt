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
import android.view.animation.LinearInterpolator
import kotlinx.android.synthetic.main.fragment_navigation_overlay.view.*
import org.mozilla.focus.ext.onGlobalLayoutOnce

private const val TRANSLATION_MILLIS_FOR_FULL_SCREEN = 400

/**
 * Encapsulation of animation code for the navigation overlay.
 */
object NavigationOverlayAnimations {

    fun onCreateViewAnimateIn(overlay: View, isOverlayOnStartup: Boolean, isBeingRestored: Boolean, onAnimationEnd: () -> Unit) {
        if (isOverlayOnStartup || isBeingRestored) {
            onAnimationEnd()
            return
        }

        // View positions are not set in onCreateView so we must wait for layout.
        overlay.onGlobalLayoutOnce {
            getAnimator(overlay, isAnimateIn = true, onAnimationEnd = onAnimationEnd).start()
        }
    }

    fun animateOut(overlay: NavigationOverlayFragment, onAnimationEnd: () -> Unit) {
        getAnimator(overlay.view!!, isAnimateIn = false, isOverlayOnStartup = overlay.isOverlayOnStartup, onAnimationEnd = onAnimationEnd)
            .start()
    }

    @Suppress("SpreadOperator") // It reduces repetition and the arrays are small so perf impact is negligible.
    private fun getAnimator(
        overlay: View,
        isAnimateIn: Boolean,
        isOverlayOnStartup: Boolean = false,
        onAnimationEnd: () -> Unit
    ): Animator {
        fun getAnimationDuration(): Long {
            // Animations feel snappy when they have a high velocity. That velocity is normally defined by the animation
            // duration for the distance traveled. Since in this view the travel distance changes based on the view
            // height (determined by the number of items), we need to set the duration dynamically based on the view
            // height. We use screen percentage traveled because it's simple and the visuals look good enough.
            val screenHeight = overlay.context.resources.displayMetrics.heightPixels
            val percentOfScreen = overlay.backgroundView.height / screenHeight.toDouble()
            return Math.round(TRANSLATION_MILLIS_FOR_FULL_SCREEN * percentOfScreen)
        }

        // Linear animations look bad for translation but the translation interpolator we use pops in too quickly when
        // executed with a short duration for alpha animations so we use different interpolators for different use cases.
        val alphaInterpolator = LinearInterpolator()
        val translationInterpolator = FastOutSlowInInterpolator()

        val fadeValues = if (isAnimateIn) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f)
        val semiOpaqueBackgroundAnimator = ObjectAnimator.ofFloat(overlay.semiOpaqueBackground, "alpha", *fadeValues).apply {
            interpolator = alphaInterpolator
        }

        val initialHomescreenBackgroundAnimator = if (!isOverlayOnStartup) {
            null
        } else {
            ObjectAnimator.ofFloat(overlay.initialHomescreenBackground, "alpha", 1f, 0f).apply {
                interpolator = alphaInterpolator
            }
        }

        val screenHeight = overlay.resources.displayMetrics.heightPixels.toFloat()
        fun getTranslateUpAnimator(view: View): Animator {
            val viewOffsetToBottomOfScreen = screenHeight - view.y
            val translateValues =
                if (isAnimateIn) floatArrayOf(viewOffsetToBottomOfScreen, 0f) else floatArrayOf(0f, viewOffsetToBottomOfScreen)
            return ObjectAnimator.ofFloat(view, "translationY", *translateValues).apply {
                interpolator = translationInterpolator
            }
        }

        return AnimatorSet().apply {
            duration = getAnimationDuration()

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

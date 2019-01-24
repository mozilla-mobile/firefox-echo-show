/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
import android.support.constraint.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_WRAP
import android.support.v4.app.Fragment
import android.support.v4.view.GestureDetectorCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_navigation_overlay.*
import kotlinx.android.synthetic.main.fragment_navigation_overlay.view.*
import kotlinx.coroutines.experimental.CancellationException
import org.mozilla.focus.R
import org.mozilla.focus.TouchInterceptorLayout
import org.mozilla.focus.UrlSearcher
import org.mozilla.focus.browser.BrowserFragmentCallbacks
import org.mozilla.focus.browser.HomeTileGridNavigation
import org.mozilla.focus.browser.HomeTileLongClickListener
import org.mozilla.focus.ext.serviceLocator
import org.mozilla.focus.ext.updateLayoutParams
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.widget.SwipeDownOutsideOfListener
import java.lang.ref.WeakReference

private const val KEY_IS_OVERLAY_ON_STARTUP = "isOverlayOnStartup"

/**
 * An overlay that allows the user to navigate to new pages including via the home tiles. The overlay
 * should be added to a fullscreen view and can be used in two modes:
 * - Initial homescreen: navigation is placed below the app bar
 * - Dialog: navigation is full screen, overlaid on a semi opaque overlay
 *
 * Note: this was originally implemented as a DialogFragment to better decouple the UI of the overlay from the
 * app but there was a long delay before it could be displayed so we switched to a regular Fragment.
 */
class NavigationOverlayFragment : Fragment() {

    val isOverlayOnStartup: Boolean by lazy { arguments!!.getBoolean(KEY_IS_OVERLAY_ON_STARTUP) }

    private val callbacks: BrowserFragmentCallbacks? get() = activity as BrowserFragmentCallbacks?

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val overlay = inflater.inflate(R.layout.fragment_navigation_overlay, container, false)

        val isVisibleInDialogMode = arrayOf(overlay.semiOpaqueBackground, overlay.dismissHitTarget, overlay.overlayHandle)
        isVisibleInDialogMode.forEach { it.visibility = if (isOverlayOnStartup) View.GONE else View.VISIBLE }
        val isVisibleInStartupMode = arrayOf(overlay.initialHomescreenBackground)
        isVisibleInStartupMode.forEach { it.visibility = if (isOverlayOnStartup) View.VISIBLE else View.GONE }

        overlay.homeTiles.urlSearcher = activity as UrlSearcher

        setOverlayHeight(overlay.homeTiles)

        NavigationOverlayAnimations.onCreateViewAnimateIn(overlay, isOverlayOnStartup, isBeingRestored = savedInstanceState != null) {
            // We defer setting click listeners until the animation completes
            // so the animation will not be interrupted.
            setOnClickListeners(overlay)
        }

        return overlay
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Since we start the async jobs in View.init and Android is inflating the view for us,
        // there's no good way to pass in the uiLifecycleJob. We could consider other solutions
        // but it'll add complexity that I don't think is probably worth it.
        homeTiles.uiLifecycleCancelJob.cancel(CancellationException("Parent lifecycle has ended"))
    }

    private fun setOverlayHeight(homeTiles: HomeTileGridNavigation) {
        homeTiles.updateLayoutParams {
            val verticalBias: Float
            val heightConstraintType: Int
            @IdRes val topToBottom: Int
            if (isOverlayOnStartup) {
                // Fill constraints from top to bottom (app bar to bottom of screen).
                verticalBias = 0f
                heightConstraintType = MATCH_CONSTRAINT_SPREAD
                topToBottom = R.id.overlayTopAsInitialHomescreen
            } else {
                // Set height based on content size, expanding up from the bottom.
                verticalBias = 1f
                heightConstraintType = MATCH_CONSTRAINT_WRAP
                topToBottom = R.id.overlayTopAsDialog
            }

            val params = it as ConstraintLayout.LayoutParams
            params.verticalBias = verticalBias
            params.matchConstraintDefaultHeight = heightConstraintType
            params.topToBottom = topToBottom
        }
    }

    private fun setOnClickListeners(overlay: View) {
        fun removeClickListeners() {
            // We remove the click listeners when dismissing the overlay
            // so that clicking them doesn't restart the animation.
            overlay.dismissHitTarget.setOnClickListener(null)
            with(overlay.homeTiles) {
                onTileClicked = null
                homeTileLongClickListener = null
            }
        }
        fun dismissOverlay() {
            removeClickListeners()
            callbacks?.setNavigationOverlayIsVisible(false)
            TelemetryWrapper.dismissHomeOverlayClickEvent()
            overlay.context.serviceLocator.swipeDownOutsideOfListener.disable()
        }

        overlay.dismissHitTarget.setOnClickListener {
            dismissOverlay()
        }

        with(overlay.homeTiles) {
            onTileClicked = {
                removeClickListeners()
                callbacks?.onNonTextInputUrlEntered(it)
            }

            homeTileLongClickListener = object : HomeTileLongClickListener {
                override fun onHomeTileLongClick(unpinTile: () -> Unit) {
                    callbacks?.onHomeTileLongClick(unpinTile)
                }
            }
        }

        overlay.context.serviceLocator
                .swipeDownOutsideOfListener.enable(overlay.homeTiles) { dismissOverlay() }
    }

    fun refreshTilesForInsertion() {
        homeTiles.refreshTilesForInsertion()
    }

    fun removePinnedSiteFromTiles(tileId: String) {
        homeTiles.removePinnedSiteFromTiles(tileId)
    }

    companion object {
        const val FRAGMENT_TAG = "navOverlay"

        fun newInstance(isOverlayOnStartup: Boolean) = NavigationOverlayFragment().apply {
            arguments = Bundle().apply {
                putBoolean(KEY_IS_OVERLAY_ON_STARTUP, isOverlayOnStartup)
            }
        }
    }
}

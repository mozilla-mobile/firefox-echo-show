/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_navigation_overlay.*
import kotlinx.android.synthetic.main.fragment_navigation_overlay.view.*
import kotlinx.coroutines.experimental.CancellationException
import org.mozilla.focus.R
import org.mozilla.focus.UrlSearcher
import org.mozilla.focus.browser.BrowserFragmentCallbacks
import org.mozilla.focus.browser.HomeTileGridNavigation
import org.mozilla.focus.browser.HomeTileLongClickListener
import org.mozilla.focus.ext.updateLayoutParams
import org.mozilla.focus.telemetry.TelemetryWrapper

private const val KEY_IS_INITIAL_HOMESCREEN = "isInitialHomescreen"

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

    private val isInitialHomescreen: Boolean by lazy { arguments!!.getBoolean(KEY_IS_INITIAL_HOMESCREEN) }

    private val callbacks: BrowserFragmentCallbacks? get() = activity as BrowserFragmentCallbacks?

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val overlay = inflater.inflate(R.layout.fragment_navigation_overlay, container, false)

        with(overlay.semiOpaqueBackground) {
            visibility = if (isInitialHomescreen) View.GONE else View.VISIBLE
            setOnClickListener {
                dismiss()
                TelemetryWrapper.dismissHomeOverlayClickEvent()
            }
        }

        with(overlay.homeTiles) {
            onTileClicked = { callbacks?.onNonTextInputUrlEntered(it) }
            urlSearcher = activity as UrlSearcher

            homeTileLongClickListener = object : HomeTileLongClickListener {
                override fun onHomeTileLongClick(unpinTile: () -> Unit) {
                    callbacks?.onHomeTileLongClick(unpinTile)
                }
            }
        }

        setOverlayHeight(overlay.homeTiles)

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
            // Since we're attached to a full screen view decoupled from the app bar, we need to use
            // the app bar height to position the navigation below the app bar.
            //
            // The current layout implementation makes setting the height through margins convenient.
            val params = it as ViewGroup.MarginLayoutParams
            params.topMargin = resources.getDimensionPixelSize(R.dimen.appbar_height)
        }
    }

    fun show(fragmentManager: FragmentManager, activity: Activity) {
        activity.getNavigationOverlayContainer().visibility = View.VISIBLE
        fragmentManager.beginTransaction()
            .replace(R.id.navigationOverlayContainer, this, FRAGMENT_TAG)
            .commit()
    }

    fun dismiss() {
        activity!!.getNavigationOverlayContainer().visibility = View.GONE
        fragmentManager!!.beginTransaction()
            .remove(this)
            .commit()
    }

    fun refreshTilesForInsertion() {
        homeTiles.refreshTilesForInsertion()
    }

    fun removePinnedSiteFromTiles(tileId: String) {
        homeTiles.removePinnedSiteFromTiles(tileId)
    }

    companion object {
        const val FRAGMENT_TAG = "navOverlay"

        fun newInstance(isInitialHomescreen: Boolean) = NavigationOverlayFragment().apply {
            arguments = Bundle().apply {
                putBoolean(KEY_IS_INITIAL_HOMESCREEN, isInitialHomescreen)
            }
        }
    }
}

private fun Activity.getNavigationOverlayContainer(): ViewGroup = findViewById(R.id.navigationOverlayContainer)

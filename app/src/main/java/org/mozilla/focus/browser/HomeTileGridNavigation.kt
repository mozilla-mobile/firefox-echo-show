/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.Context
import android.graphics.Rect
import android.preference.PreferenceManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import kotlinx.android.synthetic.main.browser_overlay.view.*
import kotlinx.android.synthetic.main.browser_overlay_top_nav.view.*
import kotlinx.coroutines.experimental.Job
import org.mozilla.focus.R
import org.mozilla.focus.ext.updateLayoutParams
import org.mozilla.focus.home.HomeTilesManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.toolbar.ToolbarStateProvider
import org.mozilla.focus.toolbar.NavigationEvent
import org.mozilla.focus.utils.Settings
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import kotlin.properties.Delegates

private const val SHOW_UNPIN_TOAST_COUNTER_PREF = "show_upin_toast_counter"
private const val MAX_UNPIN_TOAST_COUNT = 3

private const val COL_COUNT = 4

class HomeTileGridNavigation @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0 )
    : LinearLayout(context, attrs, defStyle), View.OnClickListener {

    /**
     * Used to cancel background->UI threads: we attach them as children to this job
     * and cancel this job at the end of the UI lifecycle, cancelling the children.
     */
    var uiLifecycleCancelJob: Job

    // We need this in order to show the unpin toast, at max, once per
    // instantiation of the HomeTileGridNavigation
    var canShowUpinToast: Boolean = false

    // Setting the onTileLongClick function in the HomeTileAdapter is fragile
    // since we init the tiles in View.init and Android is inflating the view for us,
    // thus we need to use Delegates.observable to update onTileLongClick.
    var openHomeTileContextMenu: (() -> Unit) by Delegates.observable({}) { _, _, newValue ->
        with (tileContainer) {
            (adapter as HomeTileAdapter).onTileLongClick = newValue
        }
    }

    var onNavigationEvent: ((event: NavigationEvent, value: String?,
                             autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) -> Unit)? = null
    /** Called inside [setVisibility] right before super.setVisibility is called. */
    var onPreSetVisibilityListener: ((isVisible: Boolean) -> Unit)? = null

    private var isTurboEnabled: Boolean
        get() = Settings.getInstance(context).isBlockingEnabled
        set(value) {
            Settings.getInstance(context).isBlockingEnabled = value
        }

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.browser_overlay, this, true)

        uiLifecycleCancelJob = Job()

        initTiles()
    }

    private fun initTiles() = with (tileContainer) {
        val homeTiles = HomeTilesManager.getTilesCache(context)

        canShowUpinToast = true

        adapter = HomeTileAdapter(uiLifecycleCancelJob, homeTiles, loadUrl = { urlStr ->
            if (urlStr.isNotEmpty()) {
                onNavigationEvent?.invoke(NavigationEvent.LOAD_TILE, urlStr, null)
            }
        }, onTileLongClick = openHomeTileContextMenu, onTileFocused = {
            val prefInt = PreferenceManager.getDefaultSharedPreferences(context).getInt(SHOW_UNPIN_TOAST_COUNTER_PREF, 0)
            if (prefInt < MAX_UNPIN_TOAST_COUNT && canShowUpinToast) {
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putInt(SHOW_UNPIN_TOAST_COUNTER_PREF, prefInt + 1)
                        .apply()
                Toast.makeText(context, R.string.homescreen_unpin_tutorial_toast, Toast.LENGTH_LONG).show()
                canShowUpinToast = false
            }
        })
        layoutManager = GridLayoutManager(context, COL_COUNT)

        // We add bottomMargin to each tile in order to add spacing between them: this makes the
        // RecyclerView slightly larger than necessary and makes the default start screen scrollable
        // even though it doesn't need to be. To undo this, we add negative margins on the tile container.
        // I tried other solutions (ItemDecoration, dynamically changing margins) but this is more
        // complex because we need to relayout more than the changed view when adding/removing a row.
        val tileBottomMargin = resources.getDimensionPixelSize(R.dimen.home_tile_margin_bottom) -
                resources.getDimensionPixelSize(R.dimen.home_tile_container_margin_bottom)
        updateLayoutParams {
            val marginLayoutParams = it as MarginLayoutParams
            marginLayoutParams.bottomMargin = -tileBottomMargin
        }
    }

    override fun onClick(view: View?) {
        val event = NavigationEvent.fromViewClick(view?.id) ?: return
        var value: String? = null

        val isTurboButtonChecked = turboButton.isChecked
        val isPinButtonChecked = pinButton.isChecked
        when (event) {
            NavigationEvent.TURBO -> {
                isTurboEnabled = isTurboButtonChecked
                value = if (isTurboButtonChecked) NavigationEvent.VAL_CHECKED
                else NavigationEvent.VAL_UNCHECKED
            }
            NavigationEvent.PIN_ACTION -> {
                value = if (isPinButtonChecked) NavigationEvent.VAL_CHECKED
                else NavigationEvent.VAL_UNCHECKED
            }
            else -> Unit // Nothing to do.
        }
        onNavigationEvent?.invoke(event, value, null)
        TelemetryWrapper.overlayClickEvent(event, isTurboButtonChecked, isPinButtonChecked)
    }

    fun getFocusedTilePosition(): Int {
        return (rootView.findFocus().parent as? RecyclerView)?.getChildAdapterPosition(rootView.findFocus()) ?: RecyclerView.NO_POSITION
    }

    override fun setVisibility(visibility: Int) {
        onPreSetVisibilityListener?.invoke(visibility == View.VISIBLE)
        super.setVisibility(visibility)

        if (visibility == View.VISIBLE) {
            tileContainer.scrollTo(0, 0)
        }
    }

    fun refreshTilesForInsertion() {
        val adapter = tileContainer.adapter as HomeTileAdapter
        adapter.updateAdapterSingleInsertion(HomeTilesManager.getTilesCache(context))
    }

    fun removePinnedSiteFromTiles(tileId: String) {
        val adapter = tileContainer.adapter as HomeTileAdapter
        adapter.removeTileFromAdapter(tileId)
    }
}

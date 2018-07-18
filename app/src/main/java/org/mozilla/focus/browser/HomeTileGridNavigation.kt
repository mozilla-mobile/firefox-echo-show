/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.Context
import android.preference.PreferenceManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.experimental.Job
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.R
import org.mozilla.focus.home.HomeTilesManager
import org.mozilla.focus.toolbar.NavigationEvent
import kotlin.properties.Delegates

private const val SHOW_UNPIN_TOAST_COUNTER_PREF = "show_upin_toast_counter"
private const val MAX_UNPIN_TOAST_COUNT = 3

private const val COL_COUNT = 4

class HomeTileGridNavigation @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0 )
    : RecyclerView(context, attrs, defStyle) {

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
        (adapter as HomeTileAdapter).onTileLongClick = newValue
    }

    var onNavigationEvent: ((event: NavigationEvent, value: String?,
                             autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) -> Unit)? = null
    /** Called inside [setVisibility] right before super.setVisibility is called. */
    var onPreSetVisibilityListener: ((isVisible: Boolean) -> Unit)? = null

    init {
        uiLifecycleCancelJob = Job()
        initTiles()
    }

    private fun initTiles() {
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
    }

    fun getFocusedTilePosition(): Int {
        return (rootView.findFocus().parent as? RecyclerView)?.getChildAdapterPosition(rootView.findFocus()) ?: RecyclerView.NO_POSITION
    }

    override fun setVisibility(visibility: Int) {
        onPreSetVisibilityListener?.invoke(visibility == View.VISIBLE)
        super.setVisibility(visibility)
        scrollTo(0, 0)
    }

    fun refreshTilesForInsertion() {
        val adapter = adapter as HomeTileAdapter
        adapter.updateAdapterSingleInsertion(HomeTilesManager.getTilesCache(context))
    }

    fun removePinnedSiteFromTiles(tileId: String) {
        val adapter = adapter as HomeTileAdapter
        adapter.removeTileFromAdapter(tileId)
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.Context
import android.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import org.mozilla.focus.R
import org.mozilla.focus.home.HomeTilesManager
import org.mozilla.focus.widget.AccessibleGridLayoutManager

private const val SHOW_UNPIN_TOAST_COUNTER_PREF = "show_upin_toast_counter"
private const val MAX_UNPIN_TOAST_COUNT = 3

class HomeTileGridNavigation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : androidx.recyclerview.widget.RecyclerView(context, attrs, defStyle) {

    // We need this in order to show the unpin toast, at max, once per
    // instantiation of the HomeTileGridNavigation
    var canShowUpinToast: Boolean = false

    // Setting the onTileLongClick function in the HomeTileAdapter is fragile
    // since we init the tiles in View.init and Android is inflating the view for us,
    // thus we need to use Delegates.observable to update onTileLongClick.
    var homeTileLongClickListener: HomeTileLongClickListener? = null
    var onTileClicked: ((value: String) -> Unit)? = null

    fun init(uiScope: CoroutineScope) {
        initTiles(uiScope)
    }

    private fun initTiles(uiScope: CoroutineScope) {
        val homeTiles = HomeTilesManager.getTilesCache(context)

        canShowUpinToast = true

        adapter = HomeTileAdapter(uiScope, homeTiles, loadUrl = { urlStr ->
            if (urlStr.isNotEmpty()) {
                onTileClicked?.invoke(urlStr)
            }
        }, onTileFocused = {
            val prefInt = PreferenceManager.getDefaultSharedPreferences(context).getInt(SHOW_UNPIN_TOAST_COUNTER_PREF, 0)
            if (prefInt < MAX_UNPIN_TOAST_COUNT && canShowUpinToast) {
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putInt(SHOW_UNPIN_TOAST_COUNTER_PREF, prefInt + 1)
                        .apply()
                Toast.makeText(context, R.string.homescreen_unpin_tutorial_toast, Toast.LENGTH_LONG).show()
                canShowUpinToast = false
            }
        }, homeTileLongClickListenerProvider = { homeTileLongClickListener })

        val tileColumnCount = resources.getInteger(R.integer.home_tile_column_count)
        layoutManager = AccessibleGridLayoutManager(context, tileColumnCount)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        scrollTo(0, 0)
    }

    fun refreshTilesForInsertion() {
        val adapter = adapter as HomeTileAdapter
        adapter.updateAdapterSingleInsertion(HomeTilesManager.getTilesCache(context))
    }

    fun removePinnedSiteFromTiles(tileId: String) {
        val adapter = adapter as HomeTileAdapter
        adapter.removeTile(tileId)
    }
}

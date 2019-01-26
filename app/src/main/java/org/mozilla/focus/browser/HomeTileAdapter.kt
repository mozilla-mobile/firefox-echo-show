/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import kotlinx.android.synthetic.main.home_tile.view.*
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.R
import org.mozilla.focus.ext.forceExhaustive
import org.mozilla.focus.ext.serviceLocator
import org.mozilla.focus.ext.toJavaURI
import org.mozilla.focus.ext.withRoundedCorners
import org.mozilla.focus.home.BundledHomeTile
import org.mozilla.focus.home.CustomHomeTile
import org.mozilla.focus.home.HomeTile
import org.mozilla.focus.home.HomeTilePlaceholderGenerator
import org.mozilla.focus.home.BundledTilesManager
import org.mozilla.focus.home.TileAction
import org.mozilla.focus.home.HomeTileScreenshotStore
import org.mozilla.focus.home.HomeTilesManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.FormattedDomain

/**
 * Duration of animation to show custom tile. If the duration is too short, the tile will just
 * pop-in. I speculate this happens because the amount of time it takes to downsample the bitmap
 * is longer than the animation duration.
 */
private const val CUSTOM_TILE_TO_SHOW_MILLIS = 200L
private val CUSTOM_TILE_ICON_INTERPOLATOR = DecelerateInterpolator()

typealias ExecuteSearch = (String, InlineAutocompleteEditText.AutocompleteResult) -> Unit

class HomeTileAdapter(
    private val uiLifecycleCancelJob: Job,
    private var tiles: MutableList<HomeTile>,
    private val loadUrl: (String) -> Unit,
    private val homeTileLongClickListenerProvider: () -> HomeTileLongClickListener?,
    var onTileFocused: (() -> Unit)?
) : RecyclerView.Adapter<TileViewHolder>() {

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) = with(holder) {
        val item = tiles[position]
        when (item) {
            is BundledHomeTile -> {
                onBindBundledHomeTile(holder, item)
                setIconLayoutMarginParams(iconView, R.dimen.home_tile_margin_value)
            }
            is CustomHomeTile -> {
                onBindCustomHomeTile(uiLifecycleCancelJob, holder, item)
                setIconLayoutMarginParams(iconView, R.dimen.home_tile_margin_value)
            }
        }.forceExhaustive

        if (item is BundledHomeTile && item.action == TileAction.SEARCH && itemView is ViewGroup) {
            itemView.setSearchClickListeners(item)

            // This removes any existing listener. setOnLongClickListener(null)
            // didn't clear the listener for an unknown reason
            itemView.setOnLongClickListener { true }
        } else {
            itemView.setNavigateClickListener(item)
            itemView.setOnLongClickListener(getDefaultLongClickListener(item))
        }

        val tvWhiteColor = ContextCompat.getColor(holder.itemView.context, R.color.tv_white)
        itemView.setOnFocusChangeListener { _, hasFocus ->
            val backgroundResource: Int
            val textColor: Int
            if (hasFocus) {
                backgroundResource = R.drawable.home_tile_title_focused_background
                textColor = tvWhiteColor
                onTileFocused?.invoke()
            } else {
                backgroundResource = 0
                textColor = Color.BLACK
            }
            titleView.setBackgroundResource(backgroundResource)
            titleView.setTextColor(textColor)
        }
    }

    private fun ViewGroup.setSearchClickListeners(item: HomeTile) {
        this.setOnClickListener {
            context.serviceLocator.pinnedTileRepo.googleSearchFocusRequest()
            TelemetryWrapper.homeTileClickEvent(item)
        }
    }

    private fun View.setNavigateClickListener(item: HomeTile) {
        this.setOnClickListener {
            loadUrl(item.url)
            TelemetryWrapper.homeTileClickEvent(item)
        }
    }

    private fun getDefaultLongClickListener(item: HomeTile) = View.OnLongClickListener {
        homeTileLongClickListenerProvider()?.onHomeTileLongClick(unpinTile = {
            HomeTilesManager.removeHomeTile(item, it.context)
            removeTile(item.idToString())
            TelemetryWrapper.homeTileRemovedEvent(item)
        })
        true
    }

    private fun setIconLayoutMarginParams(iconView: View, tileMarginValue: Int) {
        val layoutMarginParams = iconView.layoutParams as ViewGroup.MarginLayoutParams
        val marginValue = iconView.resources.getDimensionPixelSize(tileMarginValue)
        layoutMarginParams.setMargins(marginValue, marginValue, marginValue, marginValue)
        iconView.layoutParams = layoutMarginParams
    }

    /**
     * takes in the home tiles cache and updates the adapter's data source
     * and UI accordingly, assuming only one new tile is added
     */
    fun updateAdapterSingleInsertion(homeTiles: MutableList<HomeTile>) {
        if (homeTiles.size == tiles.size) {
            // The lists must not be the same size in order
            // for an insertion to be valid
            return
        }
        for ((index, tile) in tiles.withIndex()) {
            // Due to insertion, the inserted tile will be
            // the first tile that will not match the
            // previous list of tiles
            if (tile != homeTiles[index]) {
                tiles = homeTiles
                notifyItemInserted(index)
                return
            }
        }
        tiles = homeTiles
        notifyItemInserted(homeTiles.lastIndex)
    }

    fun removeTile(tileId: String) {
        for ((index, tile) in tiles.withIndex()) {
            if (tile is CustomHomeTile && tile.id.toString() == tileId || tile is BundledHomeTile && tile.id == tileId) {
                removeTile(index)
                break
            }
        }
    }

    fun removeTile(position: Int) {
        if (position > -1 && position < itemCount) {
            tiles.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount() = tiles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TileViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.home_tile, parent, false)
    )
}

private fun onBindBundledHomeTile(holder: TileViewHolder, tile: BundledHomeTile) = with(holder) {
    val bitmap = BundledTilesManager.getInstance(itemView.context).loadImageFromPath(itemView.context, tile.imagePath)
    iconView.setImageBitmap(bitmap)

    // TODO remove hardcoded search tile title
    // This is necessary while bundled tiles are loaded from JSON (which cannot
    // reference string resources)
    if (tile.action == TileAction.SEARCH) {
        titleView.text = iconView.resources.getString(R.string.google_search_tile_title)
    } else {
        titleView.text = tile.title
    }
}

private fun onBindCustomHomeTile(uiLifecycleCancelJob: Job, holder: TileViewHolder, item: CustomHomeTile) = with(holder) {
    launch(uiLifecycleCancelJob + UI, CoroutineStart.UNDISPATCHED) {
        val validUri = item.url.toJavaURI()

        val screenshotDeferred = async {
            val homeTileCornerRadius = itemView.resources.getDimension(R.dimen.home_tile_corner_radius)
            val homeTilePlaceholderCornerRadius = itemView.resources.getDimension(R.dimen.home_tile_placeholder_corner_radius)
            val screenshot = HomeTileScreenshotStore.read(itemView.context, item.id)?.withRoundedCorners(homeTileCornerRadius)
            screenshot ?: HomeTilePlaceholderGenerator.generate(itemView.context, item.url)
                    .withRoundedCorners(homeTilePlaceholderCornerRadius)
        }

        val titleDeferred = if (validUri == null) {
            CompletableDeferred(item.url)
        } else {
            async {
                val subdomainDotDomain = FormattedDomain.format(itemView.context, validUri, false, 1)
                FormattedDomain.stripCommonPrefixes(subdomainDotDomain)
            }
        }

        // We wait for both to complete so we can animate them together.
        val screenshot = screenshotDeferred.await()
        val title = titleDeferred.await()

        // NB: Don't suspend after this point (i.e. between view updates like setImage)
        // so we don't see intermediate view states.
        // TODO: It'd be less error-prone to launch { /* bg work */ launch(UI) { /* UI work */ } }
        iconView.setImageBitmap(screenshot)
        titleView.text = title

        // Animate to avoid pop-in due to thread hand-offs. TODO: animation is janky.
        AnimatorSet().apply {
            interpolator = CUSTOM_TILE_ICON_INTERPOLATOR
            duration = CUSTOM_TILE_TO_SHOW_MILLIS

            val iconAnim = ObjectAnimator.ofInt(iconView, "imageAlpha", 0, 255)
            val titleAnim = ObjectAnimator.ofFloat(titleView, "alpha", 0f, 1f)

            playTogether(iconAnim, titleAnim)
        }.start()
    }
}

class TileViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    val iconView = itemView.tile_icon
    val titleView = itemView.tile_title
}

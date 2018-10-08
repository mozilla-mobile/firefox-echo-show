/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.GridLayoutManager as AndroidGridLayoutManager

/**
 * A GridLayoutManager that fixes a11y errors in the Android framework implementation.
 */
class AccessibleGridLayoutManager(
    context: Context,
    spanCount: Int,
    orientation: Int = VERTICAL,
    reverseLayout: Boolean = false
) : AndroidGridLayoutManager(
        context,
        spanCount,
        orientation,
        reverseLayout
) {

    override fun getColumnCountForAccessibility(recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // With multiple rows, VoiceView announces "row X column Y". With a single row, VoiceView
        // announces "X of Y" where Y is the column count. However, if you have fewer than Y items
        // (i.e. 3 items for 4 columns), this is unintuitive so we override the announcement.
        return if (orientation == VERTICAL && itemCount < spanCount) {
            itemCount
        } else {
            super.getColumnCountForAccessibility(recycler, state)
        }
    }
}

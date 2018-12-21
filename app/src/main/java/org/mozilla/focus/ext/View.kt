/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver

val View.isVisible: Boolean get() = (visibility == View.VISIBLE)

/** Updates the layout params with the mutations provided to [mutateLayoutParams]. */
inline fun View.updateLayoutParams(mutateLayoutParams: (ViewGroup.LayoutParams) -> Unit) {
    val layoutParams = this.layoutParams
    mutateLayoutParams(layoutParams)
    this.layoutParams = layoutParams // Calling setLayoutParams forces them to refresh internally.
}

/**
 * Calls the given function on the next global layout call and never again.
 *
 * This may be replacable by ktx's View.doOnLayout.
 */
fun View.onGlobalLayoutOnce(onGlobalLayout: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            // We must get the view tree observer again: a view tree observer reference can become inactive
            // so we can't reuse it.
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            onGlobalLayout()
        }
    })
}

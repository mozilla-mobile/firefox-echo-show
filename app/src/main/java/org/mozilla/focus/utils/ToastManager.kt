/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import org.mozilla.focus.R

private const val TOAST_Y_OFFSET = 200

/**
 * Handles the creation and display of toasts.
 *
 * This ensures that all toasts are styled in a similar manner, and provides
 * convenience methods for frequently displayed toasts.
 */
object ToastManager {

    fun showUnpinnedToast(context: Context) {
        val brandName = context.getString(R.string.firefox_brand_name)
        context.let {
            context.getString(R.string.notification_unpinned_general2, brandName)
        }?.let { string -> showToast(string, context) }
    }

    fun showPinnedToast(context: Context) {
        val brandName = context.getString(R.string.firefox_brand_name)
        context.let {
            context.getString(R.string.notification_pinned_general2, brandName)
        }?.let { string -> showToast(string, context) }
    }

    fun showToast(text: String, context: Context) {
        context.let {
            showCenteredBottomToast(it, text,
                    0, TOAST_Y_OFFSET)
        }
    }

    fun showToast(textId: Int, context: Context) {
        context.getString(textId)?.let {
            showToast(it, context)
        }
    }
}

private fun showCenteredBottomToast(context: Context, str: CharSequence, xOffset: Int, yOffset: Int) {
    val toast = Toast.makeText(context, str, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, xOffset, yOffset)
    toast.show()
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ClipDrawable
import android.view.Gravity
import android.view.View
import org.mozilla.focus.R

/**
 * A custom view to be drawn behind the URL and page actions. Acts as the background view,
 * including while displaying loading progress.
 */
class UrlBoxBackgroundWithProgress(
    context: Context
) : View(context) {
    var progress: Int = 0
        set(value) {
            // We clip the background and progress drawable based on the new progress:
            //
            //                      progress
            //                         v
            //   +---------------------+-------------------+
            //   | background drawable | progress drawable |
            //   +---------------------+-------------------+
            //
            // The drawable is clipped completely and not visible when the level is 0 and fully
            // revealed when the level is 10,000.
            backgroundDrawable.level = 100 * (100 - value)
            progressDrawable.level = 10000 - backgroundDrawable.level
            field = value
            invalidate() // Force redraw
        }

    private var backgroundDrawable = ClipDrawable(
            resources.getDrawable(R.drawable.url_background, context.theme),
            Gravity.END,
            ClipDrawable.HORIZONTAL).apply {
        level = 10000
    }

    private var progressDrawable = ClipDrawable(
            resources.getDrawable(R.drawable.url_progress, context.theme),
            Gravity.START,
            ClipDrawable.HORIZONTAL).apply {
        level = 0
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        backgroundDrawable.setBounds(0, 0, w, h)
        progressDrawable.setBounds(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        backgroundDrawable.draw(canvas)
        progressDrawable.draw(canvas)
    }
}

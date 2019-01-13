/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import mozilla.components.support.ktx.android.content.res.pxToDp
import org.mozilla.focus.R
import org.mozilla.focus.ext.withSave

/**
 * TODO
 * - Test on device: any glitches, esp. during animation?
 * - Performance: is onDraw fast enough? How often called? Use hardware layer?
 */
class NavigationOverlayBackgroundShadowView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // todo: get from attrs
    // - foregroundColor
    // - shadowHeight
    // - ? shadowColor

    private val cornerRadius = resources.getDimensionPixelSize(R.dimen.navigation_overlay_corner_radius).toFloat()

    private val foregroundPath = Path()
    private val foregroundPaint = Paint().apply {
        isAntiAlias = true
        color = (0xFF38383D).toInt()
    }

    private val shadowHeight = resources.pxToDp(8).toFloat()
    private val shadowColor: Int = (0x66000000).toInt()
    private val shadowPath = Path()
    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK // todo
        strokeWidth = 1f // todo
        style = Paint.Style.STROKE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val widthFloat = w.toFloat()
        val heightFloat = h.toFloat()

        fun setShadowPath(): Unit = with(shadowPath) {
            reset()

            // todo: clean up style, readability
            // todo: set with foregroundPath? Or in relation to other abstractions. "guidelines"
            moveTo(0f, cornerRadius + shadowHeight)
            lineTo(0f, cornerRadius)
            arcTo(0f, 0f, cornerRadius * 2, cornerRadius * 2, 180f, 90f, false)
            lineTo(widthFloat - cornerRadius, 0f)
            arcTo(widthFloat - cornerRadius * 2, 0f, widthFloat, cornerRadius * 2, 270f, 90f, false)
            lineTo(widthFloat, cornerRadius + shadowHeight)
        }

        fun setForegroundPath(): Unit = with(foregroundPath) {
            reset()

            // TODO: offset
            addRect(cornerRadius, 0f, widthFloat - cornerRadius, cornerRadius, Path.Direction.CW)
            addRect(0f, cornerRadius, widthFloat, heightFloat, Path.Direction.CW)

            addCircle(cornerRadius, cornerRadius, cornerRadius, Path.Direction.CW) // Top left corner
            addCircle(width - cornerRadius, cornerRadius, cornerRadius, Path.Direction.CW) // Top right corner
        }

        super.onSizeChanged(w, h, oldw, oldh)

        setShadowPath()
        setForegroundPath()
    }

    override fun onDraw(canvas: Canvas) {
        fun drawShadow() {
            for (i in 0 until shadowHeight.toInt()) {
                val shadowPercent = i / (shadowHeight - 1)
                // strangely using unrelated shadowColor here.
                shadowPaint.alpha = Math.floor(Color.alpha(shadowColor) * shadowPercent.toDouble()).toInt()
                canvas.withSave{
                    canvas.translate(0f, i.toFloat())
                    val scaleX = 1f + (shadowHeight - 1 - i) * 0.00075f
                    canvas.scale(scaleX, 1f, width / 2f, 0f)
                    canvas.drawPath(shadowPath, shadowPaint)
                }
            }
        }

        fun drawForeground(): Unit = canvas.withSave {
            canvas.translate(0f, shadowHeight)
            canvas.drawPath(foregroundPath, foregroundPaint)
        }

        super.onDraw(canvas)
        drawShadow()
        drawForeground()
    }
}

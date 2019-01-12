/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.connect.firefox.integrations

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.support.annotation.CheckResult
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import java.io.File
import com.squareup.picasso.RequestCreator as PicassoRequestCreator

// via https://github.com/square/picasso/issues/332#issuecomment-30635004
private const val PATH_ANDROID_ASSET = "file:///android_asset"

// TODO: more rationale.
// TODO: write tests?
// TODO: strict mode violation.
// TODO: warm image cache on pin
// TODO: rounded image corners
// TODO: image quality
// TODO: executorService
// TODO: need coroutines dep?
/**
 * An API to load and cache images from wherever in a consistent fashion.
 *
 * We need to cache to reduce image load times (or make them synchronous) and this API prevents
 * us from having to write custom caching logic for each call site.
 *
 * Currently, this is just a wrapper around Picasso but, theoretically, this wrapper can be used to
 * swap implementations at a later point.
 */
class ImageLoader internal constructor(
    private val picasso: Picasso
) {

    fun load(path: String): RequestCreator = RequestCreator(picasso.load(path))
    fun load(file: File): RequestCreator = RequestCreator(picasso.load(file))
    fun loadFromAssets(path: String): RequestCreator = RequestCreator(picasso.load("$PATH_ANDROID_ASSET/$path"))

    fun onLowMemory() {
        picasso.clearCache() // todo: test.
    }

    class RequestCreator(private val requestCreator: PicassoRequestCreator) {
        fun into(imageView: ImageView): Unit = requestCreator.into(imageView)

        fun fit(): RequestCreator = this.also { requestCreator.fit() }
        fun centerInside(): RequestCreator = this.also { requestCreator.centerInside() }

        fun withRoundedCorners(radiusPx: Float): RequestCreator =
            this.also { requestCreator.transform(RoundCornerTransformation(radiusPx)) }
//
//        fun await(): Deferred<Boolean> {
//        }
    }

    companion object {
        fun newInstance(context: Context): ImageLoader {
            return ImageLoader(newPicassoInstance(context))
        }
    }
}

private fun newPicassoInstance(context: Context) = Picasso.Builder(context)
    .defaultBitmapConfig(Bitmap.Config.ARGB_8888)
    .build()

private fun Picasso.clearCache() {
    // TODO: have to create custom cache or access existing cache from package.
}

private class RoundCornerTransformation(
    private val radiusPx: Float
) : Transformation {

    override fun transform(source: Bitmap): Bitmap = source.withRoundedCorners(radiusPx).also {
        source.recycle()
    }

    override fun key(): String = "${javaClass.simpleName}-$radiusPx"
}

@CheckResult
fun Bitmap.withRoundedCorners(cornerRadiusPx: Float): Bitmap {
    val newBitmapWithScreenshotDims = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(newBitmapWithScreenshotDims)
    val paint = Paint()

    // Need to set isAntiAlias to true because it smooths out the edges of what is being drawn
    paint.isAntiAlias = true

    paint.shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    canvas.drawRoundRect(RectF(0.0f, 0.0f, width.toFloat(), height.toFloat()), cornerRadiusPx, cornerRadiusPx, paint)
    return newBitmapWithScreenshotDims
}

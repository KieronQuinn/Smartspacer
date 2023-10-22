package com.kieronquinn.app.smartspacer.utils.extensions

import android.graphics.*
import androidx.annotation.ColorInt
import java.io.ByteArrayOutputStream
import java.io.IOException

fun Bitmap.setTint(colour: Int): Bitmap {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(colour, PorterDuff.Mode.SRC_IN)
    val bitmap = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(bitmap)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return bitmap
}

fun Bitmap.getBackgroundColour(): Int? {
    val pixelColourCounts = HashMap<Int, Int>()
    for(y in 0 until height){
        val colour = getPixel(0, y)
        var count = pixelColourCounts[colour] ?: 0
        count++
        pixelColourCounts[colour] = count
    }
    val colour = pixelColourCounts.maxBy { it.value }.key
    return if (colour != Color.TRANSPARENT){
        colour
    } else null
}

/**
 * Compresses the bitmap to a byte array for serialization.
 */
fun Bitmap.compress(): ByteArray? {
    val out = ByteArrayOutputStream(getExpectedBitmapSize())
    return try {
        compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        out.toByteArray()
    } catch (e: IOException) {
        null
    }
}

/**
 * Try go guesstimate how much space the icon will take when serialized to avoid unnecessary
 * allocations/copies during the write (4 bytes per pixel).
 */
private fun Bitmap.getExpectedBitmapSize(): Int {
    return width * height * 4
}

/**
 * Convert square bitmap to circle
 * @param Bitmap - square bitmap
 * @return circle bitmap
 */
fun Bitmap.getRoundedBitmap(): Bitmap {
    val output = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint()
    val rect = Rect(0, 0, this.width, this.height)
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(this.width / 2f, this.height / 2f, this.width / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)
    return output
}

/**
 *  Trims a bitmap borders of a given color.
 *  https://stackoverflow.com/a/49281542/1088334
 */
fun Bitmap.trim(@ColorInt color: Int = Color.TRANSPARENT): Bitmap {
    var top = height
    var bottom = 0
    var right = width
    var left = 0

    var colored = IntArray(width) { color }
    var buffer = IntArray(width)

    for (y in bottom until top) {
        getPixels(buffer, 0, width, 0, y, width, 1)
        if (!colored.contentEquals(buffer)) {
            bottom = y
            break
        }
    }

    for (y in top - 1 downTo bottom) {
        getPixels(buffer, 0, width, 0, y, width, 1)
        if (!colored.contentEquals(buffer)) {
            top = y
            break
        }
    }

    val heightRemaining = top - bottom
    colored = IntArray(heightRemaining) { color }
    buffer = IntArray(heightRemaining)

    for (x in left until right) {
        getPixels(buffer, 0, 1, x, bottom, 1, heightRemaining)
        if (!colored.contentEquals(buffer)) {
            left = x
            break
        }
    }

    for (x in right - 1 downTo left) {
        getPixels(buffer, 0, 1, x, bottom, 1, heightRemaining)
        if (!colored.contentEquals(buffer)) {
            right = x
            break
        }
    }
    return Bitmap.createBitmap(this, left, bottom, right - left, top - bottom)
}
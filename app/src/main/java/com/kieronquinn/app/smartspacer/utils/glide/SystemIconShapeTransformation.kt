package com.kieronquinn.app.smartspacer.utils.glide

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import androidx.core.graphics.PathParser
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.kieronquinn.app.smartspacer.utils.extensions.getSize
import java.security.MessageDigest

class SystemIconShapeTransformation: BitmapTransformation() {

    companion object {
        private val ID = SystemIconShapeTransformation::class.java.name
        private val ID_BYTES = ID.toByteArray(Key.CHARSET)
    }

    private val resources = Resources.getSystem()

    private val iconShape by lazy {
        createIconShapePath()
    }

    override fun transform(
        pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int
    ): Bitmap {
        return iconShape?.let {
            val size = it.getSize()
            val scaleX = toTransform.width / size.width()
            val scaleY = toTransform.height / size.height()
            val scaleMatrix = Matrix().apply {
                setScale(scaleX, scaleY)
            }
            it.transform(scaleMatrix)
            val bitmap = Bitmap.createBitmap(
                toTransform.width, toTransform.height, toTransform.config ?: Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.clipPath(it)
            canvas.drawBitmap(toTransform, 0f, 0f, null)
            bitmap
        } ?: toTransform
    }

    override fun equals(other: Any?): Boolean {
        return other is SystemIconShapeTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    private fun createIconShapePath(): Path? {
        val iconShapeRes = resources.getIdentifier(
            "config_icon_mask", "string", "android"
        )
        if(iconShapeRes == 0) return null
        val iconShape = resources.getString(iconShapeRes)
        return PathParser.createPathFromPathData(iconShape)
    }

}
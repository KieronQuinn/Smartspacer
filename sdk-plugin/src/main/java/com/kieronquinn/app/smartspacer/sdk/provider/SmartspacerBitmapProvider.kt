package com.kieronquinn.app.smartspacer.sdk.provider

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 *  Helper class to implement a ContentProvider which can provide Smartspace Targets with image
 *  bitmaps from your app. Implementations of this class should be exported, since [openFile]
 *  verifies the call comes from Smartspacer.
 */
abstract class SmartspacerBitmapProvider: BaseProvider() {

    /**
     *  Create your [Bitmap] for a given [Uri]. You can pass parameters in the [Uri] as required,
     *  and return `null` if the Uri is somehow invalid.
     *
     *  Your Bitmap will automatically be recycled.
     */
    abstract fun getBitmap(uri: Uri): Bitmap?

    final override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        verifySecurity()
        val bitmap = getBitmap(uri)
        val bytes = bitmap?.compress() ?: return null
        val inputStream = ByteArrayInputStream(bytes)
        val pipe = ParcelFileDescriptor.createPipe()
        val outputStream = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        inputStream.copyTo(outputStream)
        return pipe[0]
    }

    /**
     * Compresses the bitmap to a byte array for serialization.
     */
    private fun Bitmap.compress(): ByteArray? {
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

    final override fun getType(uri: Uri): String {
        return "application/octet-stream"
    }

}
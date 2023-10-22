package com.kieronquinn.app.smartspacer.sdksample.plugin.providers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBitmapProvider
import com.kieronquinn.app.smartspacer.sdk.utils.createSmartspacerProxyUri
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig

/**
 *  This Provider is an example of how you can use the image URI option in the Doorbell target to
 *  load an arbitrary bitmap, but could be used to open any image. You could modify this to open
 *  an asset, or load an image from the web, etc.
 *
 *  Note that if you simply want to open a local file, you can use a regular FileProvider, although
 *  you would need to grant Uri permissions to Smartspacer's package.
 */
class ExampleLocalImageProvider: SmartspacerBitmapProvider() {

    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.imageprovider"

        /**
         *  Returns a list of Uris pointing to this provider, with the last path segment containing
         *  the number of the currently requested frame. The Uris are proxies, running via the
         *  Smartspacer proxy ContentProvider - so we don't need to grant access to every single
         *  launcher, but also don't need to leave this provider exported.
         */
        fun getUris(numberOfFrames: Int): List<Uri> {
            val uris = ArrayList<Uri>()
            for(i in 1..numberOfFrames) {
                val number = i.toString()
                val uri = Uri.Builder()
                    .scheme("content")
                    .authority(AUTHORITY)
                    .path(number)
                    .build()
                //Create a Smartspacer proxy Uri for this file Uri, so we don't need to give access to every launcher
                uris.add(createSmartspacerProxyUri(uri))
            }
            return uris
        }
    }

    override fun getBitmap(uri: Uri): Bitmap? {
        val number = uri.lastPathSegment ?: return null
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 512f, 512f, paint)
        paint.color = Color.BLACK
        paint.textSize = 256f
        paint.textAlign = Paint.Align.CENTER
        val x = canvas.width / 2f
        val y = (canvas.height / 2f - (paint.descent() + paint.ascent()) / 2)
        canvas.drawText(number, x, y, paint)
        return bitmap
    }

}
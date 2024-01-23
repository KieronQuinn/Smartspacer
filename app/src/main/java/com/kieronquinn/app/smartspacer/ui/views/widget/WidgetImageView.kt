package com.kieronquinn.app.smartspacer.ui.views.widget

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import com.kieronquinn.app.smartspacer.providers.SmartspacerElevatedProxyContentProvider.Companion.createSmartspacerElevatedProxyUri

@SuppressLint("AppCompatCustomView")
class WidgetImageView constructor(
    context: Context, attributeSet: AttributeSet?
): ImageView(context, attributeSet) {

    companion object {
        /**
         *  Naughty list of URI authorities who block access to non-system launchers
         */
        private val PROXY_AUTHORITIES = arrayOf(
            "com.spotify.mobile.android.mediaapi"
        )
    }

    @android.view.RemotableViewMethod(asyncImpl="setImageURIAsync")
    override fun setImageURI(uri: Uri?) {
        if(PROXY_AUTHORITIES.contains(uri?.authority)) {
            val proxyUri = uri?.let { createSmartspacerElevatedProxyUri(it) }
            super.setImageURI(proxyUri)
            return
        }
        super.setImageURI(uri)
    }

}
package com.kieronquinn.app.smartspacer.utils.extensions

import android.graphics.Bitmap
import android.os.Parcel
import android.widget.ImageView
import com.kieronquinn.app.smartspacer.providers.SmartspacerProxyContentProvider
import com.kieronquinn.app.smartspacer.sdk.client.views.DoubleShadowImageView
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import android.app.smartspace.uitemplatedata.Icon as SystemIcon
import android.graphics.drawable.Icon as AndroidIcon

fun Icon.toSystemIcon(): SystemIcon {
    return SystemIcon.Builder(icon)
        .setContentDescription(contentDescription)
        .setShouldTint(shouldTint)
        .build()
}

fun SystemIcon.toIcon(): Icon {
    return Icon(
        icon = icon,
        contentDescription = contentDescription,
        shouldTint = shouldTint()
    )
}

fun Icon.cloneWithTint(colour: Int): Icon? {
    val icon = icon.copy()?.apply {
        if(shouldTint){
            setTint(colour)
        }else{
            setTintList(null)
        }
    } ?: return null
    return copy(icon = icon)
}

fun AndroidIcon.cloneWithTint(colour: Int, shouldTint: Boolean): AndroidIcon? {
    return copy()?.apply {
        if(shouldTint){
            setTint(colour)
        }else{
            setTintList(null)
        }
    }
}

fun AndroidIcon.copy(): AndroidIcon? {
    return try {
        val parcel = Parcel.obtain()
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val icon = AndroidIcon.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        icon
    }catch (e: Exception){
        null
    }
}

/**
 *  Checks if the [Icon] can be loaded, since only Uri icons run through the proxy can be loaded.
 *  It *is* possible for non-proxied Uris to be loaded, but apps would need to allow all launchers,
 *  so it's limited to just proxied Uris.
 */
fun Icon.isLoadable(): Boolean {
    return when(icon.type){
        AndroidIcon.TYPE_URI, AndroidIcon.TYPE_URI_ADAPTIVE_BITMAP -> {
            icon.uri.authority == SmartspacerProxyContentProvider.AUTHORITY
        }
        else -> true
    }
}

fun Icon_createEmptyIcon(): AndroidIcon {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    return AndroidIcon.createWithBitmap(bitmap)
}

fun ImageView.setShadowEnabled(enabled: Boolean) {
    //Shadowing is only available on the DoubleShadowImageView
    if(this !is DoubleShadowImageView) return
    applyShadow = enabled
}
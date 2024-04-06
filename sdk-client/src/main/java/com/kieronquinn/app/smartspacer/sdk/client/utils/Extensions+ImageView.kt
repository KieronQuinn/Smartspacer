package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.widget.ImageView
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import android.graphics.drawable.Icon as AndroidIcon

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun ImageView.setIcon(icon: Icon?, tint: Int) {
    if(icon == null){
        setImageIcon(null)
        return
    }
    if(!icon.isLoadable()) return
    icon.icon.getEnabledDrawableOrNull(context)?.let {
        setImageDrawable(it)
    } ?: setImageIcon(icon.icon)
    contentDescription = icon.contentDescription
    imageTintList = if(icon.shouldTint){
        ColorStateList.valueOf(tint)
    }else{
        null
    }
}
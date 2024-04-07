package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon

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
package com.kieronquinn.app.smartspacer.ui.views.smartspace.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Image.Companion.EXTRA_IMAGE
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat

class CommuteTimeFeatureSmartspaceView(
    targetId: String,
    target: SmartspaceTarget,
    surface: UiSurface
): BaseFeatureSmartspaceView(targetId, target, surface) {

    override val layoutRes = R.layout.smartspace_view_feature_commute_time
    override val viewType = ViewType.FEATURE_COMMUTE_TIME

    override fun apply(context: Context, textColour: Int, remoteViews: RemoteViews, width: Int) {
        super.apply(context, textColour, remoteViews, width)
        val image = target.baseAction?.extras?.getParcelableCompat(EXTRA_IMAGE, Bitmap::class.java)
            ?: return
        val icon = Icon.createWithBitmap(image)
        remoteViews.setImageViewIcon(R.id.smartspace_view_commute_time_image, icon)
    }

}
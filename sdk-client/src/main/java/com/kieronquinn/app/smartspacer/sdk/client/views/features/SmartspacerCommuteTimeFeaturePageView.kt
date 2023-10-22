package com.kieronquinn.app.smartspacer.sdk.client.views.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageFeatureCommuteTimeBinding
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Image.Companion.EXTRA_IMAGE
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat

class SmartspacerCommuteTimeFeaturePageView(context: Context): SmartspacerBaseFeaturePageView<SmartspacePageFeatureCommuteTimeBinding>(
    context,
    SmartspacePageFeatureCommuteTimeBinding::inflate
) {

    override val title by lazy {
        binding.smartspacePageFeatureBasicTitle
    }

    override val subtitle by lazy {
        SubtitleBinding.SubtitleOnly(binding.smartspacePageFeatureBasicSubtitle)
    }

    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int
    ) {
        super.setTarget(target, interactionListener, tintColour)
        val image = target.baseAction?.extras?.getParcelableCompat(EXTRA_IMAGE, Bitmap::class.java)
            ?: return
        val icon = Icon.createWithBitmap(image)
        binding.smartspacePageCommuteTimeImage.setImageIcon(icon)
    }

}
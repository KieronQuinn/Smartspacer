package com.kieronquinn.app.smartspacer.sdk.client.views.features

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageFeatureUndefinedBinding

class SmartspacerUndefinedFeaturePageView(context: Context): SmartspacerBaseFeaturePageView<SmartspacePageFeatureUndefinedBinding>(
    context, SmartspacePageFeatureUndefinedBinding::inflate
) {

    override val title by lazy {
        binding.smartspacePageFeatureUndefinedTitle
    }

    override val subtitle by lazy {
        SubtitleBinding.SubtitleAndAction(binding.smartspacePageFeatureUndefinedSubtitle)
    }

}
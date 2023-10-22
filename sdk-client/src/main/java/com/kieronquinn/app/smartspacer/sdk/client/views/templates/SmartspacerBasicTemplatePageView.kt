package com.kieronquinn.app.smartspacer.sdk.client.views.templates

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageTemplateBasicBinding

class SmartspacerBasicTemplatePageView(context: Context): SmartspacerBaseTemplatePageView<SmartspacePageTemplateBasicBinding>(
    context,
    SmartspacePageTemplateBasicBinding::inflate
) {

    override val title by lazy {
        binding.smartspacePageTemplateBasicTitle
    }

    override val subtitle by lazy {
        SubtitleBinding.SubtitleAndAction(binding.smartspacePageTemplateBasicSubtitle)
    }

    override val supplemental by lazy {
        binding.smartspacePageTemplateBasicSupplemental
    }

}
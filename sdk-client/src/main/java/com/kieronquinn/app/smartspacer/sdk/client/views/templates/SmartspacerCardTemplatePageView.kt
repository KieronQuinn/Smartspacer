package com.kieronquinn.app.smartspacer.sdk.client.views.templates

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageTemplateCardBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.setText
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData

class SmartspacerCardTemplatePageView(context: Context): SmartspacerBaseTemplatePageView<SmartspacePageTemplateCardBinding>(
    context,
    SmartspacePageTemplateCardBinding::inflate
) {

    override val title by lazy {
        binding.smartspacePageTemplateBasicTitle
    }

    override val subtitle by lazy {
        SubtitleBinding.SubtitleOnly(binding.smartspacePageTemplateBasicSubtitle)
    }

    override val supplemental by lazy {
        binding.smartspacePageTemplateBasicSupplemental
    }

    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int
    ) {
        super.setTarget(target, interactionListener, tintColour)
        val template = target.templateData as SubCardTemplateData
        binding.smartspacePageCardText.setText(template.subCardText, tintColour)
        binding.smartspacePageCardIcon.setIcon(template.subCardIcon, tintColour)
        binding.smartspacePageCard.setOnClick(
            target, template.subCardAction, interactionListener
        )
    }

}
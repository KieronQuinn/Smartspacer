package com.kieronquinn.app.smartspacer.sdk.client.views.templates

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageTemplateHeadToHeadBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.setText
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData

class SmartspacerHeadToHeadTemplatePageView(context: Context): SmartspacerBaseTemplatePageView<SmartspacePageTemplateHeadToHeadBinding>(
    context,
    SmartspacePageTemplateHeadToHeadBinding::inflate
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
        val template = target.templateData as HeadToHeadTemplateData
        template.headToHeadTitle?.let {
            binding.smartspacePageHeadToHeadTitle.setText(it, tintColour)
        }
        template.headToHeadFirstCompetitorText?.let {
            binding.smartspacePageHeadToHead1Text.setText(it, tintColour)
        }
        template.headToHeadFirstCompetitorIcon?.let {
            binding.smartspacePageHeadToHead1Icon.setIcon(it, tintColour)
        }
        template.headToHeadSecondCompetitorText?.let {
            binding.smartspacePageHeadToHead2Text.setText(it, tintColour)
        }
        template.headToHeadSecondCompetitorIcon?.let {
            binding.smartspacePageHeadToHead2Icon.setIcon(it, tintColour)
        }
        binding.smartspacePageHeadToHead.setOnClick(
            target, template.headToHeadAction, interactionListener
        )
    }

}
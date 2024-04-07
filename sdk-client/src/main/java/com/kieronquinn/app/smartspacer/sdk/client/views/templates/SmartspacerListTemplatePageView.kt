package com.kieronquinn.app.smartspacer.sdk.client.views.templates

import android.content.Context
import android.widget.TextView
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageTemplateListBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.setText
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text

class SmartspacerListTemplatePageView(context: Context): SmartspacerBaseTemplatePageView<SmartspacePageTemplateListBinding>(
    context,
    SmartspacePageTemplateListBinding::inflate
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
        tintColour: Int,
        applyShadow: Boolean
    ) {
        super.setTarget(target, interactionListener, tintColour, applyShadow)
        val template = target.templateData as SubListTemplateData
        val items = template.subListTexts
        binding.smartspaceViewListItem1.setItem(items.getOrNull(0), tintColour, applyShadow)
        binding.smartspaceViewListItem2.setItem(items.getOrNull(1), tintColour, applyShadow)
        binding.smartspaceViewListItem3.setItem(items.getOrNull(2), tintColour, applyShadow)
        template.subListIcon?.let { binding.smartspaceViewListIcon.setIcon(it, tintColour) }
        binding.smartspaceViewListIcon.isVisible = template.subListIcon != null
        binding.smartspaceViewTemplateRoot.setOnClick(
            target, template.subListAction, interactionListener
        )
    }

    private fun TextView.setItem(item: Text?, tintColour: Int, applyShadow: Boolean) {
        isVisible = item != null
        setShadowEnabled(applyShadow)
        item?.let { setText(it, tintColour) }
    }

}
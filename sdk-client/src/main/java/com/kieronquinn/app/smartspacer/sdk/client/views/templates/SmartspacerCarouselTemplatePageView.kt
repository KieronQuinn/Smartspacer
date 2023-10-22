package com.kieronquinn.app.smartspacer.sdk.client.views.templates

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageTemplateCarouselBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.setText
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData

class SmartspacerCarouselTemplatePageView(context: Context): SmartspacerBaseTemplatePageView<SmartspacePageTemplateCarouselBinding>(
    context,
    SmartspacePageTemplateCarouselBinding::inflate
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

    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int
    ) {
        super.setTarget(target, interactionListener, tintColour)
        val templateData = target.templateData as CarouselTemplateData
        val item1 = templateData.carouselItems.getOrNull(0)
        val item2 = templateData.carouselItems.getOrNull(1)
        val item3 = templateData.carouselItems.getOrNull(2)
        val item4 = templateData.carouselItems.getOrNull(3)
        item1.setup(
            target,
            binding.smartspacePageCarouselColumn1,
            binding.smartspacePageCarouselColumn1Header,
            binding.smartspacePageCarouselColumn1Icon,
            binding.smartspacePageCarouselColumn1Footer,
            interactionListener,
            tintColour
        )
        item2.setup(
            target,
            binding.smartspacePageCarouselColumn2,
            binding.smartspacePageCarouselColumn2Header,
            binding.smartspacePageCarouselColumn2Icon,
            binding.smartspacePageCarouselColumn2Footer,
            interactionListener,
            tintColour
        )
        item3.setup(
            target,
            binding.smartspacePageCarouselColumn3,
            binding.smartspacePageCarouselColumn3Header,
            binding.smartspacePageCarouselColumn3Icon,
            binding.smartspacePageCarouselColumn3Footer,
            interactionListener,
            tintColour
        )
        item4.setup(
            target,
            binding.smartspacePageCarouselColumn4,
            binding.smartspacePageCarouselColumn4Header,
            binding.smartspacePageCarouselColumn4Icon,
            binding.smartspacePageCarouselColumn4Footer,
            interactionListener,
            tintColour
        )
        binding.smartspacePageCarousel.setOnClick(
            target,
            templateData.carouselAction,
            interactionListener
        )
    }

    private fun CarouselTemplateData.CarouselItem?.setup(
        target: SmartspaceTarget,
        column: ViewGroup,
        title: TextView,
        icon: ImageView,
        subtitle: TextView,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int
    ) {
        column.isVisible = this != null
        if(this == null) return
        upperText?.let { title.setText(it, tintColour) }
        image?.let { icon.setIcon(it, tintColour) }
        lowerText?.let { subtitle.setText(it, tintColour) }
        column.setOnClick(target, tapAction, interactionListener)
    }

}
package com.kieronquinn.app.smartspacer.sdk.client.views.templates

import android.content.Context
import android.graphics.Color
import com.kieronquinn.app.smartspacer.sdk.client.R
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageTemplateImagesBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setAspectRatio
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.whenResumed
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Images.Companion.GIF_FRAME_DURATION_MS
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Images.Companion.IMAGE_DIMENSION_RATIO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SmartspacerCardImagesPageView(context: Context): SmartspacerBaseTemplatePageView<SmartspacePageTemplateImagesBinding>(
    context,
    SmartspacePageTemplateImagesBinding::inflate
) {

    companion object {
        private const val DEFAULT_ASPECT_RATIO = "1:1"
        private const val DEFAULT_FRAME_DURATION = 1000L
    }

    override val title by lazy {
        binding.smartspacePageTemplateBasicTitle
    }

    override val subtitle by lazy {
        SubtitleBinding.SubtitleOnly(binding.smartspacePageTemplateBasicSubtitle)
    }

    override val supplemental by lazy {
        binding.smartspacePageTemplateBasicSupplemental
    }

    private var subImages: List<Icon>? = null
    private var delay: Long? = null
    private var loopJob: Job? = null
    private var tintColour = Color.TRANSPARENT

    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int
    ) {
        super.setTarget(target, interactionListener, tintColour)
        val template = target.templateData as SubImageTemplateData
        val aspectRatio = template.subImageAction?.extras
            ?.getString(IMAGE_DIMENSION_RATIO) ?: DEFAULT_ASPECT_RATIO
        val delay = template.subImageAction?.extras
            ?.getInt(GIF_FRAME_DURATION_MS)?.toLong() ?: DEFAULT_FRAME_DURATION
        binding.smartspacePageImages.setAspectRatio(R.id.smartspace_page_images_image, aspectRatio)
        binding.smartspacePageTemplateRoot.setOnClick(
            target, template.subImageAction, interactionListener
        )
        this.subImages = template.subImages
        this.delay = delay
        loopImages(tintColour)
    }

    override fun onResume() {
        super.onResume()
        loopImages(tintColour)
    }

    override fun onPause() {
        super.onPause()
        loopJob?.cancel()
    }

    @Synchronized
    private fun loopImages(tintColour: Int) {
        loopJob?.cancel()
        whenResumed {
            val subImages = subImages ?: return@whenResumed
            val delay = delay ?: return@whenResumed
            while(true){
                subImages.forEach {
                    binding.smartspacePageImagesImage.setIcon(it, tintColour)
                    delay(delay)
                }
            }
        }.also {
            loopJob = it
        }
    }

}
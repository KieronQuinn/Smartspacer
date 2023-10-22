package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Images.Companion.IMAGE_DIMENSION_RATIO
import com.kieronquinn.app.smartspacer.utils.extensions.isLoadable

class ImagesTemplateSmartspaceView(
    targetId: String,
    override val target: SmartspaceTarget,
    override val template: SubImageTemplateData,
    override val surface: UiSurface
): BaseTemplateSmartspaceView<SubImageTemplateData>(targetId, target, template, surface) {

    companion object {
        private val REGEX_ASPECT_RATIO = "([WH],)?(.*):(.*)".toRegex()
    }

    override val layoutRes = R.layout.smartspace_view_template_images
    override val viewType = ViewType.TEMPLATE_IMAGES

    @SuppressLint("InlinedApi")
    override fun apply(context: Context, textColour: Int, remoteViews: RemoteViews, width: Int) {
        super.apply(context, textColour, remoteViews, width)
        val image = template.subImages.firstOrNull()
        remoteViews.setOnClickAction(context, R.id.smartspace_view_images, template.subImageAction)
        template.subImageAction?.let {
            val aspectRatio = it.extras.getString(IMAGE_DIMENSION_RATIO) ?: return@let
            remoteViews.setAspectRatio(context, aspectRatio)
        }
        if(image?.isLoadable() == true){
            remoteViews.setViewVisibility(R.id.smartspace_view_images_image, View.VISIBLE)
            remoteViews.setImageViewIcon(
                R.id.smartspace_view_images_image, image.tintIfNeeded(textColour)
            )
        }else{
            remoteViews.setViewVisibility(R.id.smartspace_view_images_image, View.GONE)
        }
    }

    private fun RemoteViews.setAspectRatio(context: Context, ratio: String) {
        val aspectRatio = REGEX_ASPECT_RATIO.matchEntire(ratio) ?: return
        val widthRatio = aspectRatio.groupValues.getOrNull(2)?.toFloatOrNull() ?: return
        val heightRatio = aspectRatio.groupValues.getOrNull(3)?.toFloatOrNull() ?: return
        val imageWidth = context.resources.getDimension(
            R.dimen.smartspace_view_template_images_image_width
        )
        val imageHeight = context.resources.getDimension(
            R.dimen.smartspace_view_template_images_image_height
        )
        val multiplier = imageWidth / imageHeight
        if(widthRatio > heightRatio){
            //Apply to top and bottom
            val calculatedHeight = (heightRatio / widthRatio) * imageHeight
            val calculatedPadding = (((imageHeight - calculatedHeight) / 2f) / multiplier).toInt()
            setViewPadding(
                R.id.smartspace_view_images,
                0,
                calculatedPadding,
                0,
                calculatedPadding
            )
        }else{
            //Apply to left and right
            val calculatedWidth = (widthRatio / heightRatio) * imageWidth
            val calculatedPadding = (((imageWidth - calculatedWidth) / 2f) * multiplier).toInt()
            setViewPadding(
                R.id.smartspace_view_images,
                calculatedPadding,
                0,
                calculatedPadding,
                0
            )
        }
    }

}
package com.kieronquinn.app.smartspacer.ui.views.smartspace.features

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.view.View
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.providers.SmartspacerProxyContentProvider
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.DoorbellState

class DoorbellFeatureSmartspaceView(
    targetId: String,
    target: SmartspaceTarget,
    surface: UiSurface
): BaseFeatureSmartspaceView(targetId, target, surface) {

    override val layoutRes = R.layout.smartspace_view_feature_doorbell
    override val viewType = ViewType.FEATURE_DOORBELL

    val state = DoorbellState.fromTarget(target)

    override fun apply(
        context: Context,
        textColour: Int,
        shadowEnabled: Boolean,
        remoteViews: RemoteViews,
        width: Int,
        titleSize: Float,
        subtitleSize: Float,
        featureSize: Float,
        isList: Boolean,
        overflowIntent: Intent?
    ) {
        super.apply(
            context,
            textColour,
            shadowEnabled,
            remoteViews,
            width,
            titleSize,
            subtitleSize,
            featureSize,
            isList,
            overflowIntent,
        )
        val state = state ?: return
        remoteViews.clearState()
        when(state){
            is DoorbellState.Loading -> remoteViews.applyLoading(context, textColour, state)
            is DoorbellState.LoadingIndeterminate -> {
                remoteViews.applyLoadingIndeterminate(context, state)
            }
            is DoorbellState.Videocam -> remoteViews.applyVideocam(context, state)
            is DoorbellState.VideocamOff -> remoteViews.applyVideocamOff(context, state)
            is DoorbellState.ImageBitmap -> remoteViews.applyImageBitmap(context, state)
            is DoorbellState.ImageUri -> remoteViews.applyImageUri(state)
        }
    }

    private fun RemoteViews.clearState() {
        setViewVisibility(R.id.smartspace_view_doorbell_image_container, View.GONE)
        setViewVisibility(R.id.smartspace_view_doorbell_bitmap_container, View.GONE)
        setViewVisibility(R.id.smartspace_view_doorbell_icon_container, View.GONE)
        setViewVisibility(R.id.smartspace_view_doorbell_loading_container, View.GONE)
    }

    private fun RemoteViews.applyLoading(context: Context, textColour: Int, state: DoorbellState.Loading) {
        val icon = Icon.createWithBitmap(state.icon)
        if(state.tint){
            icon.setTint(textColour)
        }else{
            icon.setTintList(null)
        }
        setupIcon(context, icon, state.width, state.height, state.ratioWidth, state.ratioHeight)
        setupAspectRatio(
            context,
            R.id.smartspace_view_doorbell_loading_container,
            R.id.smartspace_view_doorbell_loading,
            state.width,
            state.height,
            state.ratioWidth,
            state.ratioHeight
        )
        if(state.showProgressBar){
            setViewVisibility(R.id.smartspace_view_doorbell_loading_container, View.VISIBLE)
            setViewVisibility(R.id.smartspace_view_doorbell_loading_background, View.GONE)
        }
    }

    private fun RemoteViews.applyLoadingIndeterminate(
        context: Context,
        state: DoorbellState.LoadingIndeterminate
    ) {
        setViewVisibility(R.id.smartspace_view_doorbell_loading_container, View.VISIBLE)
        setupAspectRatio(
            context,
            R.id.smartspace_view_doorbell_loading_container,
            R.id.smartspace_view_doorbell_loading,
            state.width,
            state.height,
            state.ratioWidth,
            state.ratioHeight
        )
    }

    private fun RemoteViews.applyVideocam(context: Context, state: DoorbellState.Videocam) {
        setViewVisibility(R.id.smartspace_view_doorbell_icon_container, View.VISIBLE)
        val icon = Icon.createWithResource(context, R.drawable.ic_target_doorbell_videocam)
        setupIcon(context, icon, state.width, state.height, state.ratioWidth, state.ratioHeight)
    }

    private fun RemoteViews.applyVideocamOff(context: Context, state: DoorbellState.VideocamOff) {
        val icon = Icon.createWithResource(context, R.drawable.ic_target_doorbell_videocam_off)
        setupIcon(context, icon, state.width, state.height, state.ratioWidth, state.ratioHeight)
    }

    private fun RemoteViews.setupIcon(
        context: Context,
        icon: Icon,
        width: Int?,
        height: Int?,
        ratioWidth: Int,
        ratioHeight: Int
    ) {
        setImageViewIcon(R.id.smartspace_view_doorbell_icon, icon)
        setViewVisibility(R.id.smartspace_view_doorbell_icon_container, View.VISIBLE)
        setupAspectRatio(
            context,
            R.id.smartspace_view_doorbell_icon_container,
            R.id.smartspace_view_doorbell_icon,
            width,
            height,
            ratioWidth,
            ratioHeight
        )
    }

    private fun RemoteViews.setupAspectRatio(
        context: Context,
        containerId: Int,
        viewId: Int,
        width: Int?,
        height: Int?,
        ratioWidth: Int,
        ratioHeight: Int
    ) {
        val maxWidth =
            context.resources.getDimension(R.dimen.smartspace_view_feature_doorbell_image_width)
        val maxHeight =
            context.resources.getDimension(R.dimen.smartspace_view_feature_doorbell_image_height)
        setAspectRatio(
            containerId,
            maxWidth,
            maxHeight,
            ratioWidth.toFloat(),
            ratioHeight.toFloat()
        )
        val iconSize =
            context.resources.getDimension(R.dimen.smartspace_view_feature_doorbell_image_height)
        val defaultPadding = context.resources.getDimension(
            R.dimen.smartspace_view_feature_doorbell_image_default_padding
        ).toInt()
        val horizontalPadding = width?.let {
            (iconSize - it) / 2f
        }?.toInt() ?: defaultPadding
        val verticalPadding = height?.let {
            (iconSize - it) / 2f
        }?.toInt() ?: defaultPadding
        setViewPadding(
            viewId,
            horizontalPadding,
            verticalPadding,
            horizontalPadding,
            verticalPadding
        )
    }

    private fun RemoteViews.applyImageBitmap(context: Context, state: DoorbellState.ImageBitmap) {
        setViewVisibility(R.id.smartspace_view_doorbell_bitmap_container, View.VISIBLE)
        val icon = Icon.createWithBitmap(state.bitmap)
        setImageViewIcon(R.id.smartspace_view_doorbell_bitmap, icon)
        val maxWidth =
            context.resources.getDimension(R.dimen.smartspace_view_feature_doorbell_image_width)
        val maxHeight =
            context.resources.getDimension(R.dimen.smartspace_view_feature_doorbell_image_height)
        val horizontalPadding = state.imageWidth?.let {
            (maxWidth - it) / 2f
        }?.toInt() ?: 0
        val verticalPadding = state.imageHeight?.let {
            (maxHeight - it) / 2f
        }?.toInt() ?: 0
        setViewPadding(
            R.id.smartspace_view_doorbell_bitmap_container,
            horizontalPadding,
            verticalPadding,
            horizontalPadding,
            verticalPadding
        )
    }

    private fun RemoteViews.applyImageUri(state: DoorbellState.ImageUri) {
        state.imageUris.firstOrNull()?.let {
            //Only attempt to show URIs loaded through the proxy since we probably can't load others
            if(it.authority != SmartspacerProxyContentProvider.AUTHORITY) {
                setViewVisibility(R.id.smartspace_view_doorbell_image_container, View.GONE)
                return
            }
            setViewVisibility(R.id.smartspace_view_doorbell_image_container, View.VISIBLE)
            setImageViewIcon(R.id.smartspace_view_doorbell_image, Icon.createWithContentUri(it))
        }
    }

    private fun RemoteViews.setAspectRatio(
        viewId: Int,
        imageWidth: Float,
        imageHeight: Float,
        widthRatio: Float,
        heightRatio: Float
    ) {
        val multiplier = imageWidth / imageHeight
        if(widthRatio > heightRatio){
            //Apply to top and bottom
            val calculatedHeight = (heightRatio / widthRatio) * imageHeight
            val calculatedPadding = (((imageHeight - calculatedHeight) / 2f) / multiplier).toInt()
            setViewPadding(viewId, 0, calculatedPadding, 0, calculatedPadding)
        }else{
            //Apply to left and right
            val calculatedWidth = (widthRatio / heightRatio) * imageWidth
            val calculatedPadding = (((imageWidth - calculatedWidth) / 2f) * multiplier).toInt()
            setViewPadding(viewId, calculatedPadding, 0, calculatedPadding, 0)
        }
    }

}
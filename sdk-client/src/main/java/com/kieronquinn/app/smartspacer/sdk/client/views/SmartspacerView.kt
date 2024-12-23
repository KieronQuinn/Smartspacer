package com.kieronquinn.app.smartspacer.sdk.client.views

import android.content.ComponentName
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.client.views.features.SmartspacerCommuteTimeFeaturePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.features.SmartspacerDoorbellFeaturePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.features.SmartspacerUndefinedFeaturePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.features.SmartspacerWeatherFeaturePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.remoteviews.SmartspacerRemoteViewsPageView
import com.kieronquinn.app.smartspacer.sdk.client.views.templates.SmartspacerBasicTemplatePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.templates.SmartspacerCardImagesPageView
import com.kieronquinn.app.smartspacer.sdk.client.views.templates.SmartspacerCardTemplatePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.templates.SmartspacerCarouselTemplatePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.templates.SmartspacerHeadToHeadTemplatePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.templates.SmartspacerListTemplatePageView
import com.kieronquinn.app.smartspacer.sdk.client.views.templates.SmartspacerWeatherTemplatePageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget.Companion.FEATURE_WEATHER
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Companion.FEATURE_ALLOWLIST_DOORBELL
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Companion.FEATURE_ALLOWLIST_IMAGE

open class SmartspacerView: FrameLayout {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    private var target: SmartspaceTarget? = null
    private var listener: SmartspaceTargetInteractionListener? = null

    private val defaultTintColour by lazy {
        context.getAttrColor(android.R.attr.textColorPrimary)
    }

    private var _tintColour: Int? = null
    private var applyShadowIfRequired: Boolean = true

    private val tintColour
        get() = _tintColour ?: defaultTintColour

    companion object {
        private const val TAG = "SmartspacerView"
    }

    init {
        setTarget(generateBlankTarget(), null)
    }

    /**
     *  Sets the Target currently shown on the View, with an optional [listener].
     *
     *  You can also pass a [tintColour], and/or [applyShadowIfRequired], which will call
     *  [setTintColour]/[setApplyShadowIfRequired] before setting the Target, but only make one
     *  update pass with the new Target.
     */
    fun setTarget(
        target: SmartspaceTarget?,
        listener: SmartspaceTargetInteractionListener?,
        tintColour: Int? = null,
        applyShadowIfRequired: Boolean? = null
    ) {
        if(tintColour != null){
            setTintColour(tintColour, false)
        }
        if(applyShadowIfRequired != null){
            setApplyShadowIfRequired(applyShadowIfRequired, false)
        }
        this.target = target
        this.listener = listener
        val view = createView(
            target ?: generateBlankTarget(),
            listener,
            this.tintColour,
            this.applyShadowIfRequired
        ) ?: createFallbackFragment(
            this.tintColour,
            this.applyShadowIfRequired
        ) ?: return
        removeAllViews()
        addView(view)
    }

    /**
     *  Sets the base tint colour of the View. This is used on icons and text which have not been
     *  tinted by plugins. This will automatically re-apply the current Target, if it is set.
     */
    fun setTintColour(tintColour: Int) {
        setTintColour(tintColour, true)
    }

    /**
     *  Sets whether to apply a shadow below text & icons in the Smartspace, for visibility on the
     *  wallpaper. Shadows are only shown on light text, they are never shown on dark. Setting
     *  this to `false` disables them entirely. This will automatically re-apply the current Target,
     *  if it is set.
     */
    fun setApplyShadowIfRequired(applyShadowIfRequired: Boolean) {
        setApplyShadowIfRequired(applyShadowIfRequired, true)
    }

    private fun setApplyShadowIfRequired(applyShadowIfRequired: Boolean, reapply: Boolean = true) {
        this.applyShadowIfRequired = applyShadowIfRequired
        if(reapply) {
            target?.let {
                setTarget(target, listener)
            }
        }
    }

    private fun setTintColour(tintColour: Int, reapply: Boolean = true) {
        _tintColour = tintColour
        if(reapply){
            target?.let {
                setTarget(target, listener)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        target?.let {
            setTarget(target, listener)
        }
    }

    private fun createView(
        target: SmartspaceTarget,
        listener: SmartspaceTargetInteractionListener?,
        tintColour: Int,
        applyShadowIfRequired: Boolean
    ): SmartspacerBasePageView<*>? {
        return try {
            val clazz = when {
                target.remoteViews != null -> SmartspacerRemoteViewsPageView::class.java
                target.templateData != null -> {
                    when(target.templateData){
                        is CarouselTemplateData -> SmartspacerCarouselTemplatePageView::class.java
                        is HeadToHeadTemplateData -> SmartspacerHeadToHeadTemplatePageView::class.java
                        is SubCardTemplateData -> SmartspacerCardTemplatePageView::class.java
                        is SubListTemplateData -> SmartspacerListTemplatePageView::class.java
                        is SubImageTemplateData -> SmartspacerCardImagesPageView::class.java
                        else -> {
                            if(target.featureType == FEATURE_WEATHER){
                                SmartspacerWeatherTemplatePageView::class.java
                            }else {
                                SmartspacerBasicTemplatePageView::class.java
                            }
                        }
                    }
                }
                else -> {
                    when{
                        FEATURE_ALLOWLIST_DOORBELL.contains(target.featureType) -> {
                            SmartspacerDoorbellFeaturePageView::class.java
                        }

                        FEATURE_ALLOWLIST_IMAGE.contains(target.featureType) -> {
                            SmartspacerCommuteTimeFeaturePageView::class.java
                        }

                        target.featureType == FEATURE_WEATHER -> {
                            SmartspacerWeatherFeaturePageView::class.java
                        }

                        else -> SmartspacerUndefinedFeaturePageView::class.java
                    }
                }
            }
            SmartspacerBasePageView.createInstance(
                context,
                clazz,
                target,
                listener,
                tintColour,
                applyShadowIfRequired
            )
        }catch (e: Exception) {
            Log.d(TAG, "Failed to create fragment for target ${target.smartspaceTargetId}", e)
            null
        }
    }

    private fun createFallbackFragment(
        tintColour: Int,
        applyShadowIfRequired: Boolean
    ): SmartspacerBasePageView<*>? {
        return createView(generateBlankTarget(), null, tintColour, applyShadowIfRequired)
    }

    private fun generateBlankTarget(): SmartspaceTarget = SmartspaceTarget(
        smartspaceTargetId = "blank_target",
        featureType = FEATURE_WEATHER,
        componentName = ComponentName("package_name", "class_name")
    )

}
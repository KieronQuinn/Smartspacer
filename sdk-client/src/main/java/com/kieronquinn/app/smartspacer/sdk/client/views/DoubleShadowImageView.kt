package com.kieronquinn.app.smartspacer.sdk.client.views

import android.content.Context
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.widget.ImageView
import androidx.annotation.RequiresApi

/**
 *  ImageView with a shadow, based on DoubleShadowIconDrawable from AOSP & SystemUI.
 *
 *  Shadow is only available on S+ with hardware acceleration, below that no shadow will be drawn.
 */
class DoubleShadowImageView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ImageView(context, attrs, defStyleAttr, defStyleRes) {

    private val mAmbientShadowRadius = 1.5f.dp
    private val mKeyShadowRadius = 0.5f.dp
    private val mKeyShadowOffsetX = 0.5f.dp
    private val mKeyShadowOffsetY = 0.5f.dp

    var applyShadow: Boolean = true

    private val mDoubleShadowNode by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            createShadowRenderNode()
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createShadowRenderNode(): RenderNode {
        val renderNode = RenderNode("DoubleShadowNode")
        renderNode.setPosition(0, 0, measuredWidth, measuredHeight)
        // Create render effects
        val ambientShadow =
            createShadowRenderEffect(
                mAmbientShadowRadius,
                0f,
                0f,
                48f
            )
        val keyShadow =
            createShadowRenderEffect(
                mKeyShadowRadius,
                mKeyShadowOffsetX,
                mKeyShadowOffsetY,
                72f
            )
        val blend = RenderEffect.createBlendModeEffect(ambientShadow, keyShadow, BlendMode.DST_ATOP)
        renderNode.setRenderEffect(blend)
        return renderNode
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createShadowRenderEffect(
        radius: Float,
        offsetX: Float,
        offsetY: Float,
        alpha: Float
    ): RenderEffect {
        return RenderEffect.createColorFilterEffect(
            PorterDuffColorFilter(Color.argb(alpha, 0f, 0f, 0f), PorterDuff.Mode.MULTIPLY),
            RenderEffect.createOffsetEffect(
                offsetX,
                offsetY,
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
        )
    }

    override fun draw(canvas: Canvas) {
        val doubleShadowNode = mDoubleShadowNode
        if (canvas.isHardwareAccelerated && applyShadow && doubleShadowNode != null) {
            if (!doubleShadowNode.hasDisplayList()) {
                // Record render node if its display list is not recorded or discarded
                // (which happens when it's no longer drawn by anything).
                val recordingCanvas = doubleShadowNode.beginRecording()
                super.draw(recordingCanvas)
                doubleShadowNode.endRecording()
            }
            canvas.drawRenderNode(doubleShadowNode)
        }
        super.draw(canvas)
    }

    private fun Resources.dip(value: Float): Float = value * displayMetrics.density

    private val Float.dp
        get() = Resources.getSystem().dip(this)
}
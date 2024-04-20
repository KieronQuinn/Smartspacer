package com.kieronquinn.app.smartspacer.sdk.client.views.base

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.whenResumed
import com.kieronquinn.app.smartspacer.sdk.client.views.DoubleShadowImageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

abstract class SmartspacerBasePageView<V: ViewBinding>(
    context: Context,
    inflate: (LayoutInflater, ViewGroup?, Boolean) -> V
): BoundView<V>(context, inflate) {

    companion object {
        fun createInstance(
            context: Context,
            clazz: Class<out SmartspacerBasePageView<*>>,
            target: SmartspaceTarget,
            listener: SmartspaceTargetInteractionListener?,
            tintColour: Int,
            applyShadowIfRequired: Boolean
        ): SmartspacerBasePageView<*> {
            return clazz.getConstructor(Context::class.java).newInstance(context).apply {
                val shouldApplyShadow = applyShadowIfRequired && tintColour == Color.WHITE
                whenResumed {
                    setTarget(target, listener, tintColour, shouldApplyShadow)
                }
            }
        }
    }

    private var target: SmartspaceTarget? = null

    abstract suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int,
        applyShadow: Boolean
    )

    protected fun ImageView.setShadowEnabled(enabled: Boolean) {
        //Shadowing is only available on the DoubleShadowImageView
        if(this !is DoubleShadowImageView) return
        applyShadow = enabled
    }

    protected fun TextView.setShadowEnabled(enabled: Boolean) {
        setShadowLayer(
            shadowRadius,
            shadowDx,
            shadowDy,
            if(enabled) Color.BLACK else Color.TRANSPARENT
        )
    }

    interface SmartspaceTargetInteractionListener {
        companion object {
            fun SmartspaceTargetInteractionListener?.launchAction(
                unlock: Boolean,
                block: () -> Unit
            ) {
                return this?.launch(unlock, block) ?: block()
            }
        }

        fun onInteraction(target: SmartspaceTarget, actionId: String?)
        fun onLongPress(target: SmartspaceTarget): Boolean
        fun launch(unlock: Boolean, block: () -> Unit) {
            return block()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun shouldTrampolineLaunches(): Boolean = false
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun trampolineLaunch(view: View, pendingIntent: PendingIntent) {}
    }

}
package com.kieronquinn.app.smartspacer.sdk.client.views.base

import android.app.PendingIntent
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.whenResumed
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
            tintColour: Int
        ): SmartspacerBasePageView<*> {
            return clazz.getConstructor(Context::class.java).newInstance(context).apply {
                whenResumed {
                    setTarget(target, listener, tintColour)
                }
            }
        }
    }

    private var target: SmartspaceTarget? = null

    abstract suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int
    )

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
        fun trampolineLaunch(pendingIntent: PendingIntent) {}
    }

}
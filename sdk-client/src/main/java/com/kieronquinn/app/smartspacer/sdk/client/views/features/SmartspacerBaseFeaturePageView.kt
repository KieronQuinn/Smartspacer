package com.kieronquinn.app.smartspacer.sdk.client.views.features

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.sdk.client.databinding.IncludeSmartspacePageSubtitleAndActionBinding
import com.kieronquinn.app.smartspacer.sdk.client.databinding.IncludeSmartspacePageSubtitleBinding
import com.kieronquinn.app.smartspacer.sdk.client.databinding.IncludeSmartspacePageTitleBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.setText
import com.kieronquinn.app.smartspacer.sdk.client.utils.shouldHeaderTintIcon
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate

abstract class SmartspacerBaseFeaturePageView<V : ViewBinding>(
    context: Context,
    inflate: (LayoutInflater, ViewGroup?, Boolean) -> V
) : SmartspacerBasePageView<V>(context, inflate) {

    abstract val title: IncludeSmartspacePageTitleBinding?
    abstract val subtitle: SubtitleBinding

    @SuppressLint("RestrictedApi")
    @CallSuper
    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int,
        applyShadow: Boolean
    ) {
        target.headerAction?.let {
            title?.smartspaceViewTitle?.setText(it.title, tintColour)
            subtitle.subtitle.setText(it.subtitle, tintColour)
            val shouldTint = target.shouldHeaderTintIcon()
            val icon = it.icon?.let { icon ->
                Icon(icon, shouldTint = shouldTint)
            }
            subtitle.subtitleIcon.setIcon(icon, tintColour)
            subtitle.subtitleIcon.setOnClick(target, it, interactionListener)
        }
        binding.root.setOnClick(target, target.headerAction, interactionListener)
        subtitle.action?.isVisible = target.baseAction?.subtitle?.isNotEmpty() == true
        subtitle.actionIcon?.isVisible = target.baseAction?.icon != null
        subtitle.root.isVisible = target.baseAction != null || target.headerAction?.subtitle != null

        target.baseAction?.let {
            subtitle.action?.setText(it.subtitle, tintColour)
            val icon = it.icon?.let { icon ->
                Icon(icon, shouldTint = ComplicationTemplate.shouldTint(it))
            }
            subtitle.actionIcon?.setIcon(icon, tintColour)
            subtitle.action?.setOnClick(target, it, interactionListener, subtitle.actionIcon)
            subtitle.actionIcon?.setOnClick(target, it, interactionListener)
        }

        title?.smartspaceViewTitle?.setShadowEnabled(applyShadow)
        subtitle.subtitle.setShadowEnabled(applyShadow)
        subtitle.subtitleIcon.setShadowEnabled(applyShadow)
        subtitle.action?.setShadowEnabled(applyShadow)
        subtitle.actionIcon?.setShadowEnabled(applyShadow)
    }

    sealed class SubtitleBinding {
        data class SubtitleOnly(
            val binding: IncludeSmartspacePageSubtitleBinding
        ) : SubtitleBinding() {
            override val root = binding.root
            override val subtitle = binding.smartspacePageSubtitleText
            override val subtitleIcon = binding.smartspacePageSubtitleIcon
            override val action: TextView? = null
            override val actionIcon: ImageView? = null
        }

        data class SubtitleAndAction(
            val binding: IncludeSmartspacePageSubtitleAndActionBinding
        ) : SubtitleBinding() {
            override val root = binding.root
            override val subtitle = binding.smartspacePageSubtitleText
            override val subtitleIcon = binding.smartspacePageSubtitleIcon
            override val action = binding.smartspacePageActionText
            override val actionIcon = binding.smartspacePageActionIcon
        }

        abstract val root: ViewGroup

        abstract val subtitle: TextView
        abstract val subtitleIcon: ImageView

        abstract val action: TextView?
        abstract val actionIcon: ImageView?
    }

}
package com.kieronquinn.app.smartspacer.sdk.client.views.templates

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
import com.kieronquinn.app.smartspacer.sdk.client.databinding.IncludeSmartspacePageSupplementalBinding
import com.kieronquinn.app.smartspacer.sdk.client.databinding.IncludeSmartspacePageTitleBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.setText
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

abstract class SmartspacerBaseTemplatePageView<V : ViewBinding>(
    context: Context,
    inflate: (LayoutInflater, ViewGroup?, Boolean) -> V
) : SmartspacerBasePageView<V>(context, inflate) {

    abstract val title: IncludeSmartspacePageTitleBinding?
    abstract val subtitle: SubtitleBinding
    abstract val supplemental: IncludeSmartspacePageSupplementalBinding

    @SuppressLint("RestrictedApi")
    @CallSuper
    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int,
        applyShadow: Boolean
    ) {
        with(target.templateData!!) {
            primaryItem?.let {
                it.text?.let { text -> title?.smartspaceViewTitle?.setText(text, tintColour) }
            }
            binding.root.setOnClick(target, primaryItem?.tapAction, interactionListener)
            title?.smartspaceViewTitle?.isVisible = primaryItem != null
            subtitleItem?.let {
                it.text?.let { text -> subtitle.subtitle.setText(text, tintColour) }
                it.icon?.let { icon -> subtitle.subtitleIcon.setIcon(icon, tintColour) }
                subtitle.subtitle.setOnClick(target, it.tapAction, interactionListener)
                subtitle.subtitleIcon.setOnClick(target, it.tapAction, interactionListener)
            }
            subtitle.root.isVisible = subtitleItem != null
            subtitle.subtitle.isVisible = subtitleItem?.text != null
            subtitle.subtitleIcon.isVisible = subtitleItem?.icon != null
            subtitleSupplementalItem?.let {
                it.text?.let { text -> subtitle.action?.setText(text, tintColour) }
                it.icon?.let { icon -> subtitle.actionIcon?.setIcon(icon, tintColour) }
                subtitle.action?.setOnClick(
                    target, it.tapAction, interactionListener, subtitle.actionIcon
                )
                subtitle.actionIcon?.setOnClick(target, it.tapAction, interactionListener)
            }
            subtitle.root.isVisible = subtitleItem != null || supplementalLineItem != null
            subtitle.action?.isVisible = subtitleSupplementalItem?.text != null
            subtitle.actionIcon?.isVisible = subtitleSupplementalItem?.icon != null
            supplementalLineItem?.let {
                it.text?.let { text ->
                    supplemental.smartspacePageSupplementalText.setText(text, tintColour)
                }
                it.icon?.let { icon ->
                    supplemental.smartspacePageSupplementalIcon.setIcon(icon, tintColour)
                }
                supplemental.smartspacePageSupplementalText
                    .setOnClick(target, it.tapAction, interactionListener)
                supplemental.smartspacePageSupplementalIcon
                    .setOnClick(target, it.tapAction, interactionListener)
            }
            supplemental.root.isVisible = supplementalLineItem != null
            supplemental.smartspacePageSupplementalText.isVisible =
                supplementalLineItem?.text != null
            supplemental.smartspacePageSupplementalIcon.isVisible =
                supplementalLineItem?.icon != null

            title?.smartspaceViewTitle?.setShadowEnabled(applyShadow)
            subtitle.subtitle.setShadowEnabled(applyShadow)
            subtitle.subtitleIcon.setShadowEnabled(applyShadow)
            supplemental.smartspacePageSupplementalText.setShadowEnabled(applyShadow)
            supplemental.smartspacePageSupplementalIcon.setShadowEnabled(applyShadow)
            subtitle.action?.setShadowEnabled(applyShadow)
            subtitle.actionIcon?.setShadowEnabled(applyShadow)
        }
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
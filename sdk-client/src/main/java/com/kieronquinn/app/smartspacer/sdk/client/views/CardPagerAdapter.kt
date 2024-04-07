package com.kieronquinn.app.smartspacer.sdk.client.views

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

class CardPagerAdapter(
    private val interactionListener: SmartspacerBasePageView.SmartspaceTargetInteractionListener
) : PagerAdapter() {

    private val targets = mutableListOf<SmartspaceTarget>()
    private var smartspaceTargets = targets
    private val holders = SparseArray<ViewHolder>()

    private var tintColour: Int? = null
    private var applyShadowIfRequired: Boolean? = null
    private var forceReload = false

    fun setTargets(
        newTargets: List<SmartspaceTarget>
    ) {
        targets.clear()
        targets.addAll(newTargets)
        notifyDataSetChanged()
    }

    fun setTintColour(tintColour: Int) {
        this.tintColour = tintColour
        forceReload = true
        notifyDataSetChanged()
    }

    fun setApplyShadowIfRequired(applyShadowIfRequired: Boolean) {
        this.applyShadowIfRequired = applyShadowIfRequired
        forceReload = true
        notifyDataSetChanged()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): ViewHolder {
        val target = smartspaceTargets[position]
        val card = createCard(container, target)
        val viewHolder = ViewHolder(position, card, target)
        onBindViewHolder(viewHolder)
        container.addView(card)
        holders.put(position, viewHolder)
        return viewHolder
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val viewHolder = obj as ViewHolder
        container.removeView(viewHolder.card)
        if (holders[position] == viewHolder) {
            holders.remove(position)
        }
    }

    fun getCardAtPosition(position: Int) = holders[position]?.card

    override fun getItemPosition(obj: Any): Int {
        val viewHolder = obj as ViewHolder
        val target = getTargetAtPosition(viewHolder.position)
        if (viewHolder.target === target && !forceReload) {
            return POSITION_UNCHANGED
        }
        forceReload = false
        if (target == null
            || getFeatureType(target) != getFeatureType(viewHolder.target)
            || target.smartspaceTargetId != viewHolder.target.smartspaceTargetId
        ) {
            return POSITION_NONE
        }
        viewHolder.target = target
        onBindViewHolder(viewHolder)
        return POSITION_UNCHANGED
    }

    fun getTargetAtPosition(position: Int): SmartspaceTarget? {
        if (position !in 0 until smartspaceTargets.size) {
            return null
        }
        return smartspaceTargets[position]
    }

    private fun onBindViewHolder(viewHolder: ViewHolder) {
        val target = smartspaceTargets[viewHolder.position]
        val card = viewHolder.card
        card.setTarget(target, interactionListener, tintColour, applyShadowIfRequired)
    }

    override fun getCount() = smartspaceTargets.size

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view === (obj as ViewHolder).card
    }

    private fun createCard(
        container: ViewGroup,
        target: SmartspaceTarget
    ): SmartspacerView {
        return SmartspacerView(container.context).apply {
            setTarget(target, interactionListener, tintColour, applyShadowIfRequired)
        }
    }

    private fun getFeatureType(target: SmartspaceTarget) = target.featureType

    class ViewHolder internal constructor(
        val position: Int,
        val card: SmartspacerView,
        var target: SmartspaceTarget
    )
}

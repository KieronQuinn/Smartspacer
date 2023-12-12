package com.kieronquinn.app.smartspacer.ui.screens.native

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.databinding.ItemNativeCompatibilityBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class NativeModeAdapter(
    recyclerView: RecyclerView,
    var items: List<CompatibilityReport>
): LifecycleAwareRecyclerView.Adapter<NativeModeAdapter.ViewHolder>(recyclerView) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNativeCompatibilityBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        nativeCompatibilityAppName.text = item.label
        glide.load(PackageIcon(item.packageName))
            .placeholder(nativeCompatibilityIcon.drawable)
            .into(nativeCompatibilityIcon)
        val adapter = NativeModeInnerAdapter(root.context, item.compatibility)
        nativeCompatibilityInner.isVisible = false
        nativeCompatibilityInner.layoutManager = LinearLayoutManager(root.context)
        nativeCompatibilityInner.adapter = adapter
        nativeCompatibilityExpand.rotation = 0f
        val toggleExpanded = {
            val expanded = !nativeCompatibilityInner.isVisible
            nativeCompatibilityInner.isVisible = expanded
            val newRotation = if(expanded) 180f else 0f
            nativeCompatibilityExpand.animate().rotation(newRotation).start()
            TransitionManager.beginDelayedTransition(root)
        }
        holder.whenResumed {
            root.onClicked().collect {
                toggleExpanded()
            }
        }
        holder.whenResumed {
            nativeCompatibilityExpand.onClicked().collect {
                toggleExpanded()
            }
        }
        Unit
    }

    data class ViewHolder(val binding: ItemNativeCompatibilityBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}
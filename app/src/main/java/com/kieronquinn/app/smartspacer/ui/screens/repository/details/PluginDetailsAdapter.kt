package com.kieronquinn.app.smartspacer.ui.screens.repository.details

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.kieronquinn.app.smartspacer.databinding.ItemPluginDetailsScreenshotBinding
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class PluginDetailsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<String>,
    private val onScreenshotClicked: (url: String) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPluginDetailsScreenshotBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder) {
        glide.load(items[position]).transition(withCrossFade()).into(binding.pluginDetailsScreenshot)
        whenResumed {
            binding.pluginDetailsScreenshot.onClicked().collect {
                onScreenshotClicked(items[position])
            }
        }
        Unit
    }

    data class ViewHolder(val binding: ItemPluginDetailsScreenshotBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}
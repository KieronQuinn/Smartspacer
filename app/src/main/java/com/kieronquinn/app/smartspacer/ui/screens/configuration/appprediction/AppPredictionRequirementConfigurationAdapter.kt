package com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.databinding.ItemAppPredictionRequirementConfigurationAppBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.repositories.PackageRepository.ListAppsApp
import com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction.AppPredictionRequirementConfigurationAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class AppPredictionRequirementConfigurationAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<ListAppsApp>,
    private val onItemClicked: (ListAppsApp) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)

    override fun getItemId(position: Int): Long {
        return items[position].packageName.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemAppPredictionRequirementConfigurationAppBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        itemAppPredictionRequirementConfigurationAppLabel.text = item.label
        itemAppPredictionRequirementConfigurationAppPackage.text = item.packageName
        itemAppPredictionRequirementConfigurationAppPackage.isVisible = item.showPackageName
        itemAppPredictionRequirementConfigurationAppIcon
        glide.load(PackageIcon(item.packageName))
            .placeholder(itemAppPredictionRequirementConfigurationAppIcon.drawable)
            .into(itemAppPredictionRequirementConfigurationAppIcon)
        holder.whenResumed {
            root.onClicked().collect {
                onItemClicked(item)
            }
        }
        Unit
    }

    data class ViewHolder(val binding: ItemAppPredictionRequirementConfigurationAppBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}
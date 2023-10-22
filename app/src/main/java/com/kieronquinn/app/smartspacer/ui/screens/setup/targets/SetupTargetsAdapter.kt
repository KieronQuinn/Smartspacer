package com.kieronquinn.app.smartspacer.ui.screens.setup.targets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemSetupTargetsTargetBinding
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class SetupTargetsAdapter(
    recyclerView: RecyclerView,
    var items: List<Item.Target>,
    private val onTargetClicked: (Item.Target) -> Unit
): LifecycleAwareRecyclerView.Adapter<SetupTargetsAdapter.ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = LayoutInflater.from(recyclerView.context)

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].authority.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSetupTargetsTargetBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        root.applyBackgroundTint(monet)
        setupTargetsTargetIcon.setImageIcon(item.icon)
        setupTargetsTargetName.text = item.label
        setupTargetsTargetDescription.text = item.description
        val addRes = if(item.isAdded){
            R.drawable.ic_setup_targets_target_added
        }else{
            R.drawable.ic_setup_targets_target_add
        }
        setupTargetsTargetAdd.setImageResource(addRes)
        if(item.isAdded){
            root.setOnClickListener(null)
            setupTargetsTargetAdd.setOnClickListener(null)
            root.alpha = 0.5f
            root.isEnabled = false
            setupTargetsTargetAdd.isEnabled = false
        }else{
            holder.whenResumed {
                root.onClicked().collect {
                    onTargetClicked(item)
                }
            }
            holder.whenResumed {
                setupTargetsTargetAdd.onClicked().collect {
                    onTargetClicked(item)
                }
            }
            root.alpha = 1f
            root.isEnabled = true
            setupTargetsTargetAdd.isEnabled = true
        }
    }

    data class ViewHolder(val binding: ItemSetupTargetsTargetBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)
}
package com.kieronquinn.app.smartspacer.ui.screens.setup.complications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemSetupComplicationsComplicationBinding
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class SetupComplicationsAdapter(
    recyclerView: RecyclerView,
    var items: List<Item.Complication>,
    private val onComplicationClicked: (Item.Complication) -> Unit
): LifecycleAwareRecyclerView.Adapter<SetupComplicationsAdapter.ViewHolder>(recyclerView) {

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
            ItemSetupComplicationsComplicationBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        root.applyBackgroundTint(monet)
        setupComplicationsComplicationIcon.setImageIcon(item.icon)
        setupComplicationsComplicationName.text = item.label
        setupComplicationsComplicationDescription.text = item.description
        val addRes = if(item.isAdded){
            R.drawable.ic_setup_targets_target_added
        }else{
            R.drawable.ic_setup_targets_target_add
        }
        setupComplicationsComplicationAdd.setImageResource(addRes)
        if(item.isAdded){
            root.setOnClickListener(null)
            setupComplicationsComplicationAdd.setOnClickListener(null)
            root.alpha = 0.5f
            root.isEnabled = false
            setupComplicationsComplicationAdd.isEnabled = false
        }else{
            holder.whenResumed {
                root.onClicked().collect {
                    onComplicationClicked(item)
                }
            }
            holder.whenResumed {
                setupComplicationsComplicationAdd.onClicked().collect {
                    onComplicationClicked(item)
                }
            }
            root.alpha = 1f
            root.isEnabled = true
            setupComplicationsComplicationAdd.isEnabled = true
        }
    }

    data class ViewHolder(val binding: ItemSetupComplicationsComplicationBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)
}
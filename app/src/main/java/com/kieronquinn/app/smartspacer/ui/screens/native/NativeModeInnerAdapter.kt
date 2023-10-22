package com.kieronquinn.app.smartspacer.ui.screens.native

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemNativeCompatibilityInnerBinding
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository

class NativeModeInnerAdapter(
    context: Context,
    var items: List<CompatibilityRepository.Compatibility>
): RecyclerView.Adapter<NativeModeInnerAdapter.ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNativeCompatibilityInnerBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        nativeCompatibilityInnerTitle.setText(item.item.title)
        nativeCompatibilityInnerContent.setText(item.item.content)
        val iconRes = if(item.compatible){
            R.drawable.ic_check_circle
        }else{
            R.drawable.ic_cross_circle
        }
        nativeCompatibilityIcon.setImageResource(iconRes)
    }

    data class ViewHolder(val binding: ItemNativeCompatibilityInnerBinding):
            RecyclerView.ViewHolder(binding.root)

}
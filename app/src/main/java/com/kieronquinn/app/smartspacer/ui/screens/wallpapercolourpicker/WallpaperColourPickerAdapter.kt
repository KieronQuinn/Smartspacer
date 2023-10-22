package com.kieronquinn.app.smartspacer.ui.screens.wallpapercolourpicker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemColorPickerBinding
import com.kieronquinn.app.smartspacer.utils.extensions.isColorDark

class WallpaperColourPickerAdapter(context: Context, private val selectedColor: Int?, private val colors: List<Int>, private val onColorPicked: (Int) -> Unit): RecyclerView.Adapter<WallpaperColourPickerAdapter.ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)

    private val checkedForeground = ContextCompat.getDrawable(
        context, R.drawable.monet_color_picker_circle_foreground_selected
    )

    override fun getItemCount() = colors.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemColorPickerBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        val isColourDark = color.isColorDark()
        val isSelected = color == selectedColor
        with(holder.binding){
            itemColorPickerBackground.backgroundTintList = ColorStateList.valueOf(color)
            itemColorPickerBackground.foreground = if(isSelected) checkedForeground else null
            itemColorPickerBackground.foregroundTintList = ColorStateList.valueOf(
                if(isColourDark) Color.WHITE else Color.BLACK
            )
            itemColorPickerCheck.isVisible = isSelected
            itemColorPickerCheck.imageTintList = ColorStateList.valueOf(
                if(isColourDark) Color.WHITE else Color.BLACK
            )
            itemColorPickerBackground.setOnClickListener {
                onColorPicked.invoke(color)
            }
        }
    }

    data class ViewHolder(val binding: ItemColorPickerBinding): RecyclerView.ViewHolder(binding.root)

}
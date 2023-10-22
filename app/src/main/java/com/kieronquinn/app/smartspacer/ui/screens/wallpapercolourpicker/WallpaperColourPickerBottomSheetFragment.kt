package com.kieronquinn.app.smartspacer.ui.screens.wallpapercolourpicker

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentWallpaperColorPickerBottomSheetBinding
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.android.ext.android.inject

class WallpaperColourPickerBottomSheetFragment :
    BaseBottomSheetFragment<FragmentWallpaperColorPickerBottomSheetBinding>(
        FragmentWallpaperColorPickerBottomSheetBinding::inflate
    ) {

    private val settings by inject<SmartspacerSettingsRepository>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = resources.getDimension(R.dimen.margin_16).toInt()
            view.updatePadding(
                left = navigationInsets.left,
                right = navigationInsets.right,
                bottom = navigationInsets.bottom + extraPadding
            )
            insets
        }
        whenResumed {
            with(binding) {
                val availableColors = monet.getAvailableWallpaperColors() ?: emptyList()
                //No available colors = likely using a live wallpaper, show a toast and dismiss
                if (availableColors.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.color_picker_unavailable),
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                    return@whenResumed
                }
                root.backgroundTintList =
                    ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
                colorPickerList.layoutManager =
                    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                colorPickerList.adapter = WallpaperColourPickerAdapter(
                    requireContext(),
                    monet.getSelectedWallpaperColor(),
                    availableColors
                ) {
                    onColorPicked(it)
                }
                colorPickerOk.setOnClickListener {
                    dialog?.dismiss()
                }
                colorPickerOk.setTextColor(monet.getAccentColor(requireContext()))
            }
        }
    }

    private fun onColorPicked(color: Int) = whenResumed {
        settings.monetColor.set(color)
        //Trigger a manual update
        monet.updateMonetColors()
    }


}
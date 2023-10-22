package com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.name

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentGeofenceRequirementConfigurationNameBottomSheetBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class GeofenceRequirementConfigurationNameBottomSheetFragment: BaseBottomSheetFragment<FragmentGeofenceRequirementConfigurationNameBottomSheetBinding>(FragmentGeofenceRequirementConfigurationNameBottomSheetBinding::inflate) {

    companion object {
        const val REQUEST_EDIT_NAME = "edit_name"
        const val KEY_NAME = "name"

        fun newInstance(name: String): GeofenceRequirementConfigurationNameBottomSheetFragment {
            return GeofenceRequirementConfigurationNameBottomSheetFragment().apply {
                arguments = bundleOf(KEY_NAME to name)
            }
        }
    }

    private val viewModel by viewModel<GeofenceRequirementConfigurationNameViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setName(arguments?.getString(KEY_NAME) ?: "")
        setupMonet()
        setupInput()
        setupOk()
        setupCancel()
        setupInsets()
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.geofenceRequirementConfigurationNamePositive.setTextColor(accent)
        binding.geofenceRequirementConfigurationNameNegative.setTextColor(accent)
        binding.geofenceRequirementConfigurationNameInput.applyMonet()
        binding.geofenceRequirementConfigurationNameEdit.applyMonet()
    }

    private fun setupInput() = with(binding.geofenceRequirementConfigurationNameEdit) {
        setText(viewModel.getName())
        whenResumed {
            onChanged().collect {
                viewModel.setName(it?.toString()?.trim() ?: "")
            }
        }
    }

    private fun setupOk() = with(binding.geofenceRequirementConfigurationNamePositive) {
        whenResumed {
            onClicked().collect {
                setFragmentResult(
                    REQUEST_EDIT_NAME,
                    bundleOf(KEY_NAME to viewModel.getName())
                )
                dismiss()
            }
        }
    }

    private fun setupCancel() = with(binding.geofenceRequirementConfigurationNameNegative) {
        whenResumed {
            onClicked().collect {
                dismiss()
            }
        }
    }

    private fun setupInsets() = with(binding.root) {
        val padding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            updatePadding(bottom = bottomInset + padding)
        }
    }

}
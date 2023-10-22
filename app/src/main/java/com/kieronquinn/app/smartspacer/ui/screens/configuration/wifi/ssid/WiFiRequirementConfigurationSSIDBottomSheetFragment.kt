package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.ssid

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationRequirementWifiSsidBottomSheetBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class WiFiRequirementConfigurationSSIDBottomSheetFragment: BaseBottomSheetFragment<FragmentConfigurationRequirementWifiSsidBottomSheetBinding>(FragmentConfigurationRequirementWifiSsidBottomSheetBinding::inflate) {

    private val viewModel by viewModel<WiFiRequirementConfigurationSSIDBottomSheetViewModel>()
    private val args by navArgs<WiFiRequirementConfigurationSSIDBottomSheetFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInput()
        setupPositive()
        setupNegative()
        setupNeutral()
        setupInsets()
        viewModel.setupWithId(args.smartspacerId)
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.configurationRequirementWifiSsidInput.applyMonet()
        binding.configurationRequirementWifiSsidEdit.applyMonet()
        binding.configurationRequirementWifiSsidPositive.setTextColor(accent)
        binding.configurationRequirementWifiSsidNegative.setTextColor(accent)
        binding.configurationRequirementWifiSsidNeutral.setTextColor(accent)
    }

    private fun setupInput() = with(binding.configurationRequirementWifiSsidEdit) {
        whenResumed {
            setText(viewModel.ssid.firstNotNull())
        }
        whenResumed {
            onChanged().collect {
                viewModel.setSSID(it?.toString() ?: "")
            }
        }
    }

    private fun setupPositive() = with(binding.configurationRequirementWifiSsidPositive) {
        whenResumed {
            onClicked().collect {
                viewModel.onPositiveClicked()
            }
        }
    }

    private fun setupNegative() = with(binding.configurationRequirementWifiSsidNegative) {
        whenResumed {
            onClicked().collect {
                viewModel.onNegativeClicked()
            }
        }
    }

    private fun setupNeutral() = with(binding.configurationRequirementWifiSsidNeutral) {
        whenResumed {
            onClicked().collect {
                viewModel.onNeutralClicked()
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
package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.mac

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.TextInputLayout
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationRequirementWifiMacBottomSheetBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.utils.input.MACAddressTextWatcher
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class WiFiRequirementConfigurationMACBottomSheetFragment: BaseBottomSheetFragment<FragmentConfigurationRequirementWifiMacBottomSheetBinding>(FragmentConfigurationRequirementWifiMacBottomSheetBinding::inflate) {

    private val viewModel by viewModel<WiFiRequirementConfigurationMACBottomSheetViewModel>()
    private val args by navArgs<WiFiRequirementConfigurationMACBottomSheetFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInput()
        setupPositive()
        setupNegative()
        setupNeutral()
        setupInsets()
        setupError()
        viewModel.setupWithId(args.smartspacerId)
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.configurationRequirementWifiMacInput.applyMonet()
        binding.configurationRequirementWifiMacEdit.applyMonet()
        binding.configurationRequirementWifiMacPositive.setTextColor(accent)
        binding.configurationRequirementWifiMacNegative.setTextColor(accent)
        binding.configurationRequirementWifiMacNeutral.setTextColor(accent)
    }

    private fun setupInput() = with(binding.configurationRequirementWifiMacEdit) {
        addTextChangedListener(MACAddressTextWatcher())
        whenResumed {
            setText(viewModel.mac.firstNotNull())
        }
        whenResumed {
            onChanged().collect {
                viewModel.setMAC(it?.toString() ?: "")
            }
        }
    }

    private fun setupError() = with(binding.configurationRequirementWifiMacInput) {
        setShowError(viewModel.showError.value)
        whenResumed {
            viewModel.showError.collect {
                setShowError(it)
            }
        }
    }

    private fun TextInputLayout.setShowError(show: Boolean) {
        isErrorEnabled = show
        error = if(show) {
            getString(R.string.requirement_wifi_configuration_mac_error)
        }else null
    }

    private fun setupPositive() = with(binding.configurationRequirementWifiMacPositive) {
        whenResumed {
            onClicked().collect {
                viewModel.onPositiveClicked()
            }
        }
    }

    private fun setupNegative() = with(binding.configurationRequirementWifiMacNegative) {
        whenResumed {
            onClicked().collect {
                viewModel.onNegativeClicked()
            }
        }
    }

    private fun setupNeutral() = with(binding.configurationRequirementWifiMacNeutral) {
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
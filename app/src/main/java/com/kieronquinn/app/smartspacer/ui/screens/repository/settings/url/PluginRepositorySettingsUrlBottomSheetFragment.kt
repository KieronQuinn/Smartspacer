package com.kieronquinn.app.smartspacer.ui.screens.repository.settings.url

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSettingsPluginRepositoryUrlBottomSheetBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class PluginRepositorySettingsUrlBottomSheetFragment: BaseBottomSheetFragment<FragmentSettingsPluginRepositoryUrlBottomSheetBinding>(FragmentSettingsPluginRepositoryUrlBottomSheetBinding::inflate) {

    private val viewModel by viewModel<PluginRepositorySettingsUrlBottomSheetViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInput()
        setupPositive()
        setupNegative()
        setupNeutral()
        setupInsets()
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.settingsPluginRepositoryUrlInput.applyMonet()
        binding.settingsPluginRepositoryUrlEdit.applyMonet()
        binding.settingsPluginRepositoryUrlPositive.setTextColor(accent)
        binding.settingsPluginRepositoryUrlNegative.setTextColor(accent)
        binding.settingsPluginRepositoryUrlNeutral.setTextColor(accent)
    }

    private fun setupInput() = with(binding.settingsPluginRepositoryUrlEdit) {
        setText(viewModel.url)
        whenResumed {
            onChanged().collect {
                viewModel.setUrl(it?.toString() ?: "")
            }
        }
    }

    private fun setupPositive() = with(binding.settingsPluginRepositoryUrlPositive) {
        whenResumed {
            onClicked().collect {
                viewModel.onPositiveClicked()
            }
        }
    }

    private fun setupNegative() = with(binding.settingsPluginRepositoryUrlNegative) {
        whenResumed {
            onClicked().collect {
                viewModel.onNegativeClicked()
            }
        }
    }

    private fun setupNeutral() = with(binding.settingsPluginRepositoryUrlNeutral) {
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
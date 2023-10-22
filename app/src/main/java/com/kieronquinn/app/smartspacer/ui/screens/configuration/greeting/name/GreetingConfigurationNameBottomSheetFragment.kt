package com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.name

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationTargetGreetingNameBottomSheetBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.name.GreetingConfigurationNameBottomSheetViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import org.koin.androidx.viewmodel.ext.android.viewModel

class GreetingConfigurationNameBottomSheetFragment: BaseBottomSheetFragment<FragmentConfigurationTargetGreetingNameBottomSheetBinding>(FragmentConfigurationTargetGreetingNameBottomSheetBinding::inflate) {

    private val viewModel by viewModel<GreetingConfigurationNameBottomSheetViewModel>()
    private val args by navArgs<GreetingConfigurationNameBottomSheetFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupState()
        setupInput()
        setupPositive()
        setupNegative()
        setupInsets()
        viewModel.setupWithId(args.smartspacerId)
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.settingsNameInput.applyMonet()
        binding.settingsNameEdit.applyMonet()
        binding.settingsNamePositive.setTextColor(accent)
        binding.settingsNameNegative.setTextColor(accent)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            //We only want to update the loaded state once, with the initial value
            viewModel.state.filter { it is State.Loaded }.take(1).collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        if(state !is State.Loaded) return
        binding.settingsNameEdit.setText(state.name)
    }

    private fun setupInput() = with(binding.settingsNameEdit) {
        whenResumed {
            onChanged().collect {
                viewModel.setName(it?.toString() ?: "")
            }
        }
    }

    private fun setupPositive() = with(binding.settingsNamePositive) {
        whenResumed {
            onClicked().collect {
                viewModel.onPositiveClicked()
            }
        }
    }

    private fun setupNegative() = with(binding.settingsNameNegative) {
        whenResumed {
            onClicked().collect {
                viewModel.onNegativeClicked()
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
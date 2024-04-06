package com.kieronquinn.app.smartspacer.ui.screens.configuration.date.custom

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationTargetDateFormatCustomBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateTargetConfigurationFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.custom.DateFormatCustomViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import org.koin.androidx.viewmodel.ext.android.viewModel

class DateFormatCustomFragment: BaseBottomSheetFragment<FragmentConfigurationTargetDateFormatCustomBinding>(FragmentConfigurationTargetDateFormatCustomBinding::inflate) {

    private val viewModel by viewModel<DateFormatCustomViewModel>()
    private val args by navArgs<DateFormatCustomFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInput()
        setupState()
        setupPositive()
        setupNegative()
        setupInsets()
        viewModel.setup(args.format ?: "")
    }

    private fun setupInput() = with(binding.dateFormatCustomEdit) {
        handleInput(viewModel.state.value)
        applyMonet()
        binding.dateFormatCustomInput.applyMonet()
        whenResumed {
            //We only want to update the loaded state once, with the initial value
            viewModel.state.filter { it is State.Loaded }.take(1).collect {
                handleInput(it)
            }
        }
        whenResumed {
            onChanged().collect {
                viewModel.setFormat(it?.toString() ?: "")
            }
        }
    }

    private fun handleInput(state: State) = with(binding) {
        if(state !is State.Loaded) return
        dateFormatCustomEdit.setText(state.format)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        if(state !is State.Loaded) return
        if(state.date != null) {
            dateFormatCustomInput.isHelperTextEnabled = true
            dateFormatCustomInput.isErrorEnabled = false
            dateFormatCustomInput.helperText = state.date
            dateFormatCustomPositive.isEnabled = true
            dateFormatCustomPositive.alpha = 1f
        }else{
            dateFormatCustomInput.isHelperTextEnabled = false
            dateFormatCustomInput.isErrorEnabled = true
            dateFormatCustomInput.error = getString(R.string.target_date_settings_custom_invalid)
            dateFormatCustomPositive.isEnabled = false
            dateFormatCustomPositive.alpha = 0.5f
        }
    }

    private fun setupPositive() = with(binding.dateFormatCustomPositive) {
        setTextColor(monet.getAccentColor(requireContext()))
        whenResumed {
            onClicked().collect {
                onPositiveClicked()
            }
        }
    }

    private fun onPositiveClicked() {
        val state = viewModel.state.value as? State.Loaded ?: return
        if(state.date != null) {
            setFragmentResult(
                DateTargetConfigurationFragment.REQUEST_KEY_DATE_FORMAT,
                bundleOf(
                    DateTargetConfigurationFragment.RESULT_KEY_DATE_FORMAT to state.format
                )
            )
            dismiss()
        }
    }

    private fun setupNegative() = with(binding.dateFormatCustomNegative) {
        setTextColor(monet.getAccentColor(requireContext()))
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
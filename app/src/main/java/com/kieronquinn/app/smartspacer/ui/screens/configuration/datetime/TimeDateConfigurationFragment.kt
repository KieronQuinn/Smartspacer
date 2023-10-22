package com.kieronquinn.app.smartspacer.ui.screens.configuration.datetime

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentTimeDateRequirementConfigurationBinding
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.configuration.datetime.TimeDateConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.toDate
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

class TimeDateConfigurationFragment: BoundFragment<FragmentTimeDateRequirementConfigurationBinding>(FragmentTimeDateRequirementConfigurationBinding::inflate), BackAvailable, LockCollapsed {

    private val viewModel by viewModel<TimeDateConfigurationViewModel>()

    private val id
        get() = requireActivity().intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)!!

    private val timeFormat by lazy {
        DateFormat.getTimeFormat(requireContext())
    }

    private val chipMapping by lazy {
        mapOf(
            binding.requirementTimeDateConfigurationChipMonday to DayOfWeek.MONDAY,
            binding.requirementTimeDateConfigurationChipTuesday to DayOfWeek.TUESDAY,
            binding.requirementTimeDateConfigurationChipWednesday to DayOfWeek.WEDNESDAY,
            binding.requirementTimeDateConfigurationChipThursday to DayOfWeek.THURSDAY,
            binding.requirementTimeDateConfigurationChipFriday to DayOfWeek.FRIDAY,
            binding.requirementTimeDateConfigurationChipSaturday to DayOfWeek.SATURDAY,
            binding.requirementTimeDateConfigurationChipSunday to DayOfWeek.SUNDAY,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupState()
        setupChips()
        setupStartTime()
        setupEndTime()
        setupFab()
        setupError()
        viewModel.setupWithId(id)
        binding.dateTimeRequirementConfigurationScrollable.isNestedScrollingEnabled = false
    }

    private fun setupMonet() {
        val background = monet.getBackgroundColor(requireContext())
        val primary = monet.getPrimaryColor(requireContext())
        binding.root.setBackgroundColor(background)
        val toolbar = monet.getBackgroundColorSecondary(requireContext()) ?: primary
        binding.dateTimeRequirementConfigurationLoading.loadingProgress.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        val buttonTint = ColorStateList.valueOf(accent)
        val chipBackground = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(toolbar, primary)
        )
        binding.requirementTimeDateConfigurationStartTimeButton.backgroundTintList = buttonTint
        binding.requirementTimeDateConfigurationStartTimeButton.setTextColor(accent)
        binding.requirementTimeDateConfigurationEndTimeButton.backgroundTintList = buttonTint
        binding.requirementTimeDateConfigurationEndTimeButton.setTextColor(accent)
        binding.requirementTimeDateConfigurationFabSave.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        chipMapping.keys.forEach {
            it.chipBackgroundColor = chipBackground
        }
    }

    private fun setupChips() {
        chipMapping.forEach {
            it.key.text = it.value.getDisplayName(TextStyle.FULL, Locale.getDefault())
            whenResumed {
                it.key.onClicked().collect { _ ->
                    viewModel.onChipClicked(it.value)
                }
            }
        }
    }

    private fun setupStartTime() = with(binding.requirementTimeDateConfigurationStartTimeButton) {
        whenResumed {
            onClicked().collect {
                val current = (viewModel.state.value as? State.Loaded)?.startTime ?: return@collect
                showPicker(
                    getString(R.string.requirement_time_date_configuration_start_time),
                    current,
                    viewModel::onStartTimeSelected
                )
            }
        }
    }

    private fun setupEndTime() = with(binding.requirementTimeDateConfigurationEndTimeButton) {
        whenResumed {
            onClicked().collect {
                val current = (viewModel.state.value as? State.Loaded)?.endTime ?: return@collect
                showPicker(
                    getString(R.string.requirement_time_date_configuration_end_time),
                    current,
                    viewModel::onEndTimeSelected
                )
            }
        }
    }

    private fun setupFab() = with(binding.requirementTimeDateConfigurationFabSave) {
        whenResumed {
            onClicked().collect {
                viewModel.onSaveClicked()
            }
        }
        val defaultMargin = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                updateMargins(bottom = insets.getInsets(Type.systemBars()).bottom + defaultMargin)
            }
        }
    }

    private fun setupError() = whenResumed {
        viewModel.errorBus.collect {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(it)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.dateTimeRequirementConfigurationLoading.root.isVisible = true
                binding.dateTimeRequirementConfigurationLoaded.isVisible = false
            }
            is State.Loaded -> {
                binding.dateTimeRequirementConfigurationLoading.root.isVisible = false
                binding.dateTimeRequirementConfigurationLoaded.isVisible = true
                binding.requirementTimeDateConfigurationStartTimeButton.text = state.startTime.format()
                binding.requirementTimeDateConfigurationEndTimeButton.text = state.endTime.format()
                chipMapping.forEach {
                    it.key.isChecked = state.selectedDays.contains(it.value)
                }
            }
            is State.Success -> {
                requireActivity().run {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun LocalTime.format(): String {
        return timeFormat.format(this.atDate(LocalDate.now()).toDate())
    }

    private fun showPicker(
        title: CharSequence,
        current: LocalTime,
        onPositive: (LocalTime) -> Unit
    ) {
        val timeFormat = if(DateFormat.is24HourFormat(requireContext())){
            TimeFormat.CLOCK_24H
        }else{
            TimeFormat.CLOCK_12H
        }
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(timeFormat)
            .setHour(current.hour)
            .setMinute(current.minute)
            .setTitleText(title)
            .build()
        picker.addOnPositiveButtonClickListener {
            onPositive(LocalTime.of(picker.hour, picker.minute))
        }
        picker.show(childFragmentManager, "picker")
    }

}
package com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentBatteryOptimisationBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation.BatteryOptimisationViewModel.BatteryOptimisationSettingsItem
import com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation.BatteryOptimisationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet

abstract class BatteryOptimisationFragment: BoundFragment<FragmentBatteryOptimisationBinding>(FragmentBatteryOptimisationBinding::inflate) {

    abstract val viewModel: BatteryOptimisationViewModel
    abstract val showControls: Boolean

    private val adapter by lazy {
        BatteryOptimisationAdapter(binding.batteryOptimisationRecyclerview, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupRecyclerView()
        setupControls()
        setupLoading()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun setupRecyclerView() = with(binding.batteryOptimisationRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@BatteryOptimisationFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupLoading() = with(binding.batteryOptimisationLoading.loadingProgress) {
        applyMonet()
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
                binding.batteryOptimisationLoading.root.isVisible = true
                binding.batteryOptimisationRecyclerview.isVisible = false
                binding.batteryOptimisationControls.isVisible = false
            }
            is State.Loaded -> {
                binding.batteryOptimisationLoading.root.isVisible = false
                binding.batteryOptimisationRecyclerview.isVisible = true
                binding.batteryOptimisationControls.isVisible =
                    state.batteryOptimisationsDisabled && showControls
                onAcceptabilityChanged(state.batteryOptimisationsDisabled)
                adapter.update(loadItems(state), binding.batteryOptimisationRecyclerview)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val optimisationSwitch = GenericSettingsItem.SwitchSetting(
            state.batteryOptimisationsDisabled,
            getString(R.string.battery_optimisation_system_title),
            getString(R.string.battery_optimisation_system_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_battery_saver),
            !state.batteryOptimisationsDisabled
        ) {
            viewModel.onBatteryOptimisationClicked()
        }
        val oemSetting = if(state.oemBatteryOptimisationAvailable){
            GenericSettingsItem.Setting(
                getString(R.string.battery_optimisation_oem_title),
                getString(R.string.battery_optimisation_oem_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_open),
                onClick = viewModel::onOemBatteryOptimisationClicked
            )
        }else null
        val footer = BatteryOptimisationSettingsItem.Footer(
            viewModel::onLearnMoreClicked
        )
        return listOfNotNull(optimisationSwitch, oemSetting, footer)
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.batteryOptimisationControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.batteryOptimisationControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.batteryOptimisationControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.batteryOptimisationControlsNext.onClicked().collect {
                viewModel.moveToNext()
            }
        }
    }

    open fun onAcceptabilityChanged(acceptable: Boolean) {
        //No-op by default
    }

}
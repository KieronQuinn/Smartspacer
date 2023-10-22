package com.kieronquinn.app.smartspacer.ui.base.settings.radio

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

abstract class BaseRadioSettingsFragment<T : Enum<T>>: BaseSettingsFragment() {

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    open val header: List<BaseSettingsItem> = emptyList()

    abstract val viewModel: BaseRadioSettingsViewModel<T>

    abstract fun getSettingTitle(setting: T): CharSequence
    abstract fun getSettingContent(setting: T): CharSequence
    abstract fun getValues(): List<T>

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
        when(state) {
            is State.Loading -> {
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded<*> -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.getItems(), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded<*>.getItems(): List<BaseSettingsItem> {
        return header + getValues().map {
            GenericSettingsItem.RadioCard(
                it == setting,
                getSettingTitle(it),
                getSettingContent(it)
            ) { viewModel.onSettingClicked(it) }
        }
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}
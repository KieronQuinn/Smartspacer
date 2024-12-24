package com.kieronquinn.app.smartspacer.ui.screens.configuration.widgettarget.configuration

import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.targets.WidgetTarget.TargetData.Padding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.widgettarget.configuration.WidgetTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WidgetTargetConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    private val viewModel by viewModel<WidgetTargetConfigurationViewModel> {
        parametersOf(requireActivity().intent.getStringExtra(EXTRA_SMARTSPACER_ID))
    }

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

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> {
                settingsBaseRecyclerView.isVisible = false
                settingsBaseLoading.isVisible = true
            }
            is State.Loaded -> {
                settingsBaseRecyclerView.isVisible = true
                settingsBaseLoading.isVisible = false
                adapter.update(state.loadItems(), settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> = listOfNotNull(
        GenericSettingsItem.Card(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
            createInfoText()
        ),
        GenericSettingsItem.Setting(
            getString(R.string.target_widget_settings_widget_title),
            getString(R.string.target_widget_settings_widget_content, data.label, data.appName),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_widgets),
            isEnabled = false,
            onClick = {}
        ),
        GenericSettingsItem.SwitchSetting(
            data.rounded,
            getString(R.string.target_widget_settings_rounded_title),
            getString(R.string.target_widget_settings_rounded_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_custom_widget_options_round_corners),
            onChanged = viewModel::onRoundChanged
        ).takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
        GenericSettingsItem.Dropdown(
            getString(R.string.target_widget_settings_padding_title),
            getString(R.string.target_widget_settings_padding_content, getString(data.padding.label)),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_widget_configuration_padding),
            data.padding,
            viewModel::onPaddingChanged,
            Padding.entries
        ) {
            it.label
        },
        GenericSettingsItem.SwitchSetting(
            data.useAlternativeSizing,
            getString(R.string.target_widget_alt_sizing_title),
            getText(R.string.target_widget_alt_sizing_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_widget_configuration_sizing),
            onChanged = viewModel::onAltSizingChanged
        )
    )

    private fun createInfoText() = SpannableStringBuilder().apply {
        val footerItems = resources.getStringArray(R.array.target_widget_settings_limitations)
        appendLine(getString(R.string.target_widget_settings_limitations_content))
        footerItems.forEachIndexed { i, item ->
            appendBullet()
            append(item)
            if(i < footerItems.size - 1) {
                appendLine()
            }
        }
        appendLine()
        appendLine()
        append(getText(R.string.target_widget_settings_limitations_footer))
    }

    private fun SpannableStringBuilder.appendBullet() {
        append(
            " ",
            BulletSpan(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}
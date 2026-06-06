package com.kieronquinn.app.smartspacer.ui.screens.settings.complicationonprimary

import androidx.core.content.ContextCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ComplicationOnPrimary
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsComplicationOnPrimaryFragment: BaseRadioSettingsFragment<ComplicationOnPrimary>(), BackAvailable {

    override val viewModel by viewModel<SettingsComplicationOnPrimaryViewModel>()

    override val header by lazy {
        listOf(
            GenericSettingsItem.Card(
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                content = getText(R.string.settings_complication_on_primary_info),
                topPadding = 0
            ),
            GenericSettingsItem.Header(null, true)
        )
    }

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override fun getSettingTitle(setting: ComplicationOnPrimary): CharSequence {
        return getString(setting.label)
    }

    override fun getSettingContent(setting: ComplicationOnPrimary): CharSequence {
        return getText(setting.content)
    }

    override fun getValues(): List<ComplicationOnPrimary> {
        return ComplicationOnPrimary.entries
    }

}
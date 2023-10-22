package com.kieronquinn.app.smartspacer.ui.screens.native.settings.pagelimit

import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TargetCountLimit
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class NativeModePageLimitFragment: BaseRadioSettingsFragment<TargetCountLimit>(), BackAvailable, HideBottomNavigation {

    private val args by navArgs<NativeModePageLimitFragmentArgs>()

    override val viewModel by viewModel<NativeModePageLimitViewModel>()

    override val header by lazy {
        listOf(getHeader())
    }

    override fun getSettingTitle(setting: TargetCountLimit): CharSequence {
        return getString(setting.label)
    }

    override fun getSettingContent(setting: TargetCountLimit): CharSequence {
        return getText(setting.content)
    }

    override fun getValues(): List<TargetCountLimit> {
        return TargetCountLimit.values().asList()
    }

    override fun shouldHideBottomNavigation(): Boolean {
        return args.isFromSettings
    }

    private fun getHeader(): GenericSettingsItem.Card {
        return GenericSettingsItem.Card(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning),
            getText(R.string.native_mode_settings_target_limit_warning)
        )
    }

}
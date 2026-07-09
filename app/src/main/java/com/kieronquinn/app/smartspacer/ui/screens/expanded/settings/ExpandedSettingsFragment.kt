package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.model.expanded.NavItemDisplayMode
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.ExpandedSettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExpandedSettingsFragment : BaseSettingsFragment(), BackAvailable, HideBottomNavigation {

    private val viewModel by viewModel<ExpandedSettingsViewModel>()
    private val args by navArgs<ExpandedSettingsFragmentArgs>()

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
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
        when (state) {
            is State.Loading -> {
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.loadItems())
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        val footer = GenericSettingsItem.Footer(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_mode),
            getString(R.string.expanded_settings_footer)
        )
        val header = GenericSettingsItem.Switch(
            enabled,
            getString(R.string.expanded_settings_switch),
            if (readYouAvailable) viewModel::onEnabledChanged else { _ -> }
        )
        if (!enabled) {
            return listOf(header, footer)
        }
        val backgroundBlurCompatible = viewModel.isBackgroundBlurCompatible()
        return listOf(
            header,
            GenericSettingsItem.Header(getString(R.string.expanded_settings_header_behaviour)),
            GenericSettingsItem.Setting(
                getString(R.string.expanded_settings_open_mode_home_screen_title),
                getString(
                    R.string.expanded_settings_open_mode_home_screen_content,
                    getString(openModeHome.label)
                ),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_show_on_home),
                onClick = { viewModel.onOpenModeHomeClicked(args.isFromSettings) }
            ),
            GenericSettingsItem.Setting(
                getString(R.string.expanded_settings_open_mode_lock_screen_title),
                getString(
                    R.string.expanded_settings_open_mode_lock_screen_content,
                    getString(openModeLock.label)
                ),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_show_on_lockscreen),
                onClick = { viewModel.onOpenModeLockClicked(args.isFromSettings) }
            ),
            GenericSettingsItem.SwitchSetting(
                closeWhenLocked,
                getString(R.string.expanded_settings_close_when_locked_title),
                getString(R.string.expanded_settings_close_when_locked_content),
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_settings_expanded_close_when_locked
                ),
                onChanged = viewModel::onCloseWhenLockedChanged
            ),
            GenericSettingsItem.SwitchSetting(
                showDoodle,
                getString(R.string.expanded_settings_show_doodle_title),
                getString(R.string.expanded_settings_show_doodle_content),
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_target_at_a_glance
                ),
                onChanged = viewModel::onShowDoodleChanged
            ),
            GenericSettingsItem.SwitchSetting(
                doodleOpenGoogleApp,
                getString(R.string.expanded_settings_doodle_open_google_app_title),
                getString(R.string.expanded_settings_doodle_open_google_app_content),
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_google_logo_monet
                ),
                enabled = showDoodle,
                onChanged = viewModel::onDoodleOpenGoogleAppChanged
            ),
            GenericSettingsItem.SwitchSetting(
                xposedEnabled && xposedAvailable,
                getString(R.string.expanded_settings_xposed_enabled_title),
                if(xposedAvailable) {
                    getText(R.string.expanded_settings_xposed_enabled_content)
                }else{
                    getString(R.string.expanded_settings_xposed_enabled_content_disabled)
                },
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_xposed
                ),
                enabled = xposedAvailable
            ) {
                viewModel.onXposedEnabledChanged(requireContext(), it)
            },
            GenericSettingsItem.Header(getString(R.string.expanded_settings_header_style)),
            GenericSettingsItem.SwitchSetting(
                showWeatherCookie,
                getString(R.string.expanded_settings_weather_cookie_title),
                getString(R.string.expanded_settings_weather_cookie_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_complication_google_weather),
                onChanged = viewModel::onShowWeatherCookieChanged
            ),
            GenericSettingsItem.Dropdown(
                getString(R.string.expanded_settings_background_mode_title),
                getString(
                    R.string.expanded_settings_background_mode_content,
                    getString(backgroundMode.label)
                ),
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_settings_expanded_blur_background
                ),
                backgroundMode,
                onSet = viewModel::onBackgroundModeChanged,
                SmartspacerSettingsRepository.ExpandedBackground.getAvailable(backgroundBlurCompatible)
            ) {
                it.label
            },
            GenericSettingsItem.Dropdown(
                getString(R.string.expanded_settings_nav_bar_style_title),
                getString(R.string.expanded_settings_nav_bar_style_content, getString(navItemDisplayMode.label)),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_tint_colour),
                navItemDisplayMode,
                viewModel::onNavItemDisplayModeChanged,
                NavItemDisplayMode.entries
            ) { it.label },
            footer
        )
    }

    override fun shouldHideBottomNavigation(): Boolean {
        return !args.isFromSettings
    }

    inner class Adapter : BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}
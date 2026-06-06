package com.kieronquinn.app.smartspacer.ui.screens.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate.BlurMode
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Header
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.SwitchSetting
import com.kieronquinn.app.smartspacer.ui.base.CanShowSnackbar
import com.kieronquinn.app.smartspacer.ui.base.Root
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.settings.SettingsViewModel.SettingsSettingsItem
import com.kieronquinn.app.smartspacer.ui.screens.settings.SettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.getSelectedLanguage
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment: BaseSettingsFragment(), Root, CanShowSnackbar {

    private val viewModel by viewModel<SettingsViewModel>()

    private val blur by lazy {
        BlurDelegate.get(BlurMode.None(
            requireView(),
            requireActivity().window,
            requireContext()
        ), lifecycleScope)
    }

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        SettingsAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupBlur()
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
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.getItems(), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun setupBlur() = whenCreated {
        blur.isBlurAvailable(requireContext()).collect {
            viewModel.onBlurAvailableChanged(it)
        }
    }

    private fun State.Loaded.getItems(): List<BaseSettingsItem> = listOfNotNull(
        Setting(
            getString(R.string.settings_enhanced_title),
            if(enhancedCompatible){
                getString(R.string.settings_enhanced_content)
            }else{
                getString(R.string.settings_enhanced_content_unsupported)
            },
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_enhanced),
            isEnabled = enhancedCompatible,
            onClick = viewModel::onEnhancedClicked
        ),
        Setting(
            getString(R.string.settings_native_smartspace_settings_title),
            when {
                !supportsNativeSmartspace -> {
                    getString(R.string.settings_native_smartspace_settings_content_incompatible)
                }
                !enhancedEnabled || !enhancedCompatible -> {
                    getString(R.string.settings_native_smartspace_settings_content_incompatible_enhanced)
                }
                else -> {
                    getString(R.string.settings_native_smartspace_settings_content)
                }
            },
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_native_smartspace),
            isEnabled = supportsNativeSmartspace && enhancedCompatible && enhancedEnabled,
            onClick = viewModel::onNativeClicked
        ),
        Setting(
            getString(R.string.oem_smartspace_title),
            if(enhancedEnabled && enhancedCompatible) {
                getString(R.string.oem_smartspace_content)
            }else{
                getString(R.string.oem_smartspace_content_enhanced)
            },
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_oem_smartspace),
            isEnabled = enhancedCompatible && enhancedEnabled,
            onClick = viewModel::onOemSmartspaceClicked
        ),
        Setting(
            getString(R.string.notification_widget_title),
            getString(R.string.notification_widget_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_show_on_lockscreen),
            onClick = viewModel::onNotificationWidgetClicked
        ),
        Setting(
            getString(R.string.expanded_settings_title),
            getString(R.string.expanded_settings_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_mode),
            onClick = viewModel::onExpandedModeClicked
        ),
        Header(getString(R.string.settings_targets_complications)),
        Setting(
            getString(R.string.settings_hide_sensitive_contents_title),
            getString(R.string.settings_hide_sensitive_contents_content, getString(hideSensitive.label)),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_hide_sensitive_content),
            onClick = viewModel::onHideSensitiveContentClicked,
        ),
        Setting(
            getString(R.string.settings_complication_on_primary_title),
            getString(R.string.settings_complication_on_primary_content, getString(complicationOnPrimary.label)),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_complications),
            onClick =viewModel::onComplicationOnPrimaryClicked
        ),
        Header(getString(R.string.settings_plugins)),
        Setting(
            getString(R.string.plugin_settings_title),
            getString(R.string.plugin_settings_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_plugins),
            onClick = viewModel::onPluginRepositoryClicked,
        ),
        Setting(
            getString(R.string.permissions_title),
            getString(R.string.permissions_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_permissions),
            onClick = viewModel::onPermissionsClicked,
        ),
        Header(getString(R.string.settings_options)),
        Setting(
            getString(R.string.backup_restore_title),
            getString(R.string.backup_restore_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_backup_restore),
            onClick = viewModel::onBackupRestoreClicked
        ),
        Setting(
            getString(R.string.battery_optimisation_title),
            getString(R.string.battery_optimisation_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_battery_saver),
            onClick = viewModel::onBatteryOptimisationClicked,
        ),
        SwitchSetting(
            checkForUpdates,
            getString(R.string.settings_enable_update_check_title),
            getString(R.string.settings_enable_update_check_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_updates),
            onChanged = viewModel::onCheckForUpdatesChanged
        ),
        SwitchSetting(
            enableAnalytics,
            getString(R.string.settings_enable_analytics_title),
            getString(R.string.settings_enable_analytics_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_analytics),
            onChanged = viewModel::onEnableAnalyticsChanged
        ),
        if (blurAvailable != null) {
            SwitchSetting(
                blurEnabled && blurAvailable,
                getString(R.string.settings_enable_blur_title),
                if (blurAvailable) {
                    getString(R.string.settings_enable_blur_content)
                } else {
                    getString(R.string.settings_enable_blur_content_disabled)
                },
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_expanded_blur_background),
                onChanged = viewModel::onEnableBlurChanged,
                enabled = blurAvailable
            )
        } else null,
        Setting(
            getString(R.string.settings_language_title),
            requireContext().getSelectedLanguage(supportedLocales)?.displayName
                ?: getString(R.string.settings_language_default),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_language),
            onClick = viewModel::onLanguageClicked
        ),
        Header(getString(R.string.settings_debug_header)),
        Setting(
            getString(R.string.settings_dump_title),
            getString(R.string.settings_dump_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_debug),
            onClick = viewModel::onDebugClicked,
        ),
        SettingsSettingsItem.About(
            viewModel::onContributorsClicked,
            viewModel::onDonateClicked,
            viewModel::onGitHubClicked,
            viewModel::onCrowdinClicked,
            viewModel::onBlueskyClicked,
            viewModel::onXdaClicked,
            viewModel::onLibrariesClicked
        )
    )

}
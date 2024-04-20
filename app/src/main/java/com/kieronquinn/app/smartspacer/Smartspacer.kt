package com.kieronquinn.app.smartspacer

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kieronquinn.app.smartspacer.components.blur.BlurProvider
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigationImpl
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigationImpl
import com.kieronquinn.app.smartspacer.components.navigation.ExpandedNavigation
import com.kieronquinn.app.smartspacer.components.navigation.ExpandedNavigationImpl
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigationImpl
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigationImpl
import com.kieronquinn.app.smartspacer.components.navigation.WidgetOptionsNavigation
import com.kieronquinn.app.smartspacer.components.navigation.WidgetOptionsNavigationImpl
import com.kieronquinn.app.smartspacer.components.smartspace.SmartspaceManager
import com.kieronquinn.app.smartspacer.model.database.SmartspacerDatabase
import com.kieronquinn.app.smartspacer.repositories.AccessibilityRepository
import com.kieronquinn.app.smartspacer.repositories.AccessibilityRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import com.kieronquinn.app.smartspacer.repositories.AlarmRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.AnalyticsRepository
import com.kieronquinn.app.smartspacer.repositories.AnalyticsRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.AppPredictionRepository
import com.kieronquinn.app.smartspacer.repositories.AppPredictionRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.BackupRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.BatteryOptimisationRepository
import com.kieronquinn.app.smartspacer.repositories.BatteryOptimisationRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepository
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.BroadcastRepository
import com.kieronquinn.app.smartspacer.repositories.BroadcastRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository
import com.kieronquinn.app.smartspacer.repositories.CalendarRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.CallsRepository
import com.kieronquinn.app.smartspacer.repositories.CallsRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.DataRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository
import com.kieronquinn.app.smartspacer.repositories.DownloadRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.FlashlightRepository
import com.kieronquinn.app.smartspacer.repositories.FlashlightRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.GeofenceRepository
import com.kieronquinn.app.smartspacer.repositories.GeofenceRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.GmailRepository
import com.kieronquinn.app.smartspacer.repositories.GmailRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.MediaRepository
import com.kieronquinn.app.smartspacer.repositories.MediaRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.PluginRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.RecentTasksRepository
import com.kieronquinn.app.smartspacer.repositories.RecentTasksRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.SmsRepository
import com.kieronquinn.app.smartspacer.repositories.SmsRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.UpdateRepository
import com.kieronquinn.app.smartspacer.repositories.UpdateRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepositoryImpl
import com.kieronquinn.app.smartspacer.ui.activities.MainActivityViewModel
import com.kieronquinn.app.smartspacer.ui.activities.MainActivityViewModelImpl
import com.kieronquinn.app.smartspacer.ui.activities.WidgetOptionsMenuViewModel
import com.kieronquinn.app.smartspacer.ui.activities.WidgetOptionsMenuViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.BackupRestoreViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.BackupRestoreViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.backup.BackupViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.backup.BackupViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.RestoreViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.RestoreViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complications.RestoreComplicationsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complications.RestoreComplicationsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.settings.RestoreSettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.settings.RestoreSettingsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.targets.RestoreTargetsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.targets.RestoreTargetsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.widgets.RestoreWidgetsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.widgets.RestoreWidgetsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.complications.add.ComplicationsAddViewModel
import com.kieronquinn.app.smartspacer.ui.screens.complications.add.ComplicationsAddViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.complications.edit.ComplicationEditViewModel
import com.kieronquinn.app.smartspacer.ui.screens.complications.edit.ComplicationEditViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.ComplicationsRequirementsPageViewModel
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.ComplicationsRequirementsPageViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.ComplicationsRequirementsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.ComplicationsRequirementsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add.ComplicationsRequirementsAddViewModel
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add.ComplicationsRequirementsAddViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction.AppPredictionRequirementConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction.AppPredictionRequirementConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.blank.BlankTargetConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.blank.BlankTargetConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth.BluetoothRequirementConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth.BluetoothRequirementConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.calendar.CalendarTargetConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.calendar.CalendarTargetConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateComplicationConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateComplicationConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateTargetConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateTargetConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.custom.DateFormatCustomViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.custom.DateFormatCustomViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.picker.DateFormatPickerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.picker.DateFormatPickerViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.datetime.TimeDateConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.datetime.TimeDateConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.default.DefaultTargetConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.default.DefaultTargetConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.flashlight.FlashlightTargetConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.flashlight.FlashlightTargetConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.GeofenceRequirementConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.GeofenceRequirementConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.name.GeofenceRequirementConfigurationNameViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.name.GeofenceRequirementConfigurationNameViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.gmail.GmailComplicationConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.gmail.GmailComplicationConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.GreetingConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.GreetingConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.name.GreetingConfigurationNameBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.name.GreetingConfigurationNameBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.music.MusicConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.music.MusicConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.NotificationTargetConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.NotificationTargetConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker.NotificationTargetConfigurationAppPickerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker.NotificationTargetConfigurationAppPickerViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.RecentTaskRequirementConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.RecentTaskRequirementConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.apppicker.RecentTaskRequirementConfigurationAppPickerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.apppicker.RecentTaskRequirementConfigurationAppPickerViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.limit.RecentTaskRequirementConfigurationLimitBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.limit.RecentTaskRequirementConfigurationLimitBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.widget.WidgetConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.widget.WidgetConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.WiFiRequirementConfigurationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.WiFiRequirementConfigurationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.mac.WiFiRequirementConfigurationMACBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.mac.WiFiRequirementConfigurationMACBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.picker.WiFiRequirementConfigurationPickerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.picker.WiFiRequirementConfigurationPickerViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.ssid.WiFiRequirementConfigurationSSIDBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.ssid.WiFiRequirementConfigurationSSIDBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.container.ContainerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.container.ContainerViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.contributors.ContributorsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.contributors.ContributorsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.donate.DonateViewModel
import com.kieronquinn.app.smartspacer.ui.screens.donate.DonateViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.EnhancedModeViewModel
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.EnhancedModeViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.request.EnhancedModeRequestViewModel
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.request.EnhancedModeRequestViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.expanded.options.ExpandedWidgetOptionsBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.options.ExpandedWidgetOptionsBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange.ExpandedRearrangeViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange.ExpandedRearrangeViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.ExpandedSettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.ExpandedSettingsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.home.ExpandedHomeOpenModeSettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.lock.ExpandedLockOpenModeSettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.searchprovider.ExpandedSettingsSearchProviderViewModel
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.searchprovider.ExpandedSettingsSearchProviderViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.native.NativeModeViewModel
import com.kieronquinn.app.smartspacer.ui.screens.native.NativeModeViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.native.reconnect.NativeReconnectViewModel
import com.kieronquinn.app.smartspacer.ui.screens.native.reconnect.NativeReconnectViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.native.settings.NativeModeSettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.native.settings.NativeModeSettingsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.native.settings.pagelimit.NativeModePageLimitViewModel
import com.kieronquinn.app.smartspacer.ui.screens.notificationwidget.NotificationWidgetSettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.notificationwidget.NotificationWidgetSettingsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace.SettingsOemSmartspaceViewModel
import com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace.SettingsOemSmartspaceViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.permission.DisplayOverOtherAppsPermissionBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.permission.DisplayOverOtherAppsPermissionBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.repository.PluginRepositoryViewModel
import com.kieronquinn.app.smartspacer.ui.screens.repository.PluginRepositoryViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.repository.settings.PluginRepositorySettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.repository.settings.PluginRepositorySettingsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.repository.settings.url.PluginRepositorySettingsUrlBottomSheetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.repository.settings.url.PluginRepositorySettingsUrlBottomSheetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.settings.SettingsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.settings.SettingsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.settings.batteryoptimisation.SettingsBatteryOptimisationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.settings.batteryoptimisation.SettingsBatteryOptimisationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.settings.dump.DumpSmartspacerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.settings.dump.DumpSmartspacerViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.settings.sensitive.SettingsHideSensitiveViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.analytics.SetupAnalyticsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.analytics.SetupAnalyticsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.batteryoptimisation.SetupBatteryOptimisationViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.batteryoptimisation.SetupBatteryOptimisationViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.complete.SetupCompleteViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.complete.SetupCompleteViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.complicationinfo.SetupComplicationInfoViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.complicationinfo.SetupComplicationInfoViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.complications.SetupComplicationsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.complications.SetupComplicationsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.decision.SetupDecisionViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.decision.SetupDecisionViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.expanded.SetupExpandedSmartspaceViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.expanded.SetupExpandedSmartspaceViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.landing.SetupLandingViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.landing.SetupLandingViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.notifications.SetupNotificationsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.notifications.SetupNotificationsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.plugins.SetupPluginsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.plugins.SetupPluginsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.requirements.SetupRequirementsInfoViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.requirements.SetupRequirementsInfoViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.targetinfo.SetupTargetInfoViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.targetinfo.SetupTargetInfoViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.targets.SetupTargetsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.targets.SetupTargetsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.setup.widget.SetupWidgetViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.widget.SetupWidgetViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddViewModel
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.targets.edit.TargetEditViewModel
import com.kieronquinn.app.smartspacer.ui.screens.targets.edit.TargetEditViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.TargetsRequirementsPageViewModel
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.TargetsRequirementsPageViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.TargetsRequirementsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.TargetsRequirementsViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.add.TargetsRequirementsAddViewModel
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.add.TargetsRequirementsAddViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.update.UpdateViewModel
import com.kieronquinn.app.smartspacer.ui.screens.update.UpdateViewModelImpl
import com.kieronquinn.app.smartspacer.utils.extensions.gsonExclusionStrategy
import com.kieronquinn.app.smartspacer.utils.gson.LocalTimeAdapter
import com.kieronquinn.monetcompat.core.MonetCompat
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.sui.Sui
import java.time.LocalDate

class Smartspacer: Application(), Configuration.Provider {

    companion object {
        private const val PACKAGE_KEYGUARD_DEFAULT = "com.android.systemui"

        /**
         *  Finds the Keyguard package (usually SystemUI) from the framework. If the config value
         *  isn't found or isn't set somehow, it will default to [PACKAGE_KEYGUARD_DEFAULT].
         *
         *  This handles some OEMs who use a custom SystemUI package.
         */
        @SuppressLint("DiscouragedApi")
        private fun findKeyguard(): String {
            val resources = Resources.getSystem()
            val configKeyguard = resources.getIdentifier(
                "config_keyguardComponent", "string", "android"
            )
            if(configKeyguard == 0) return PACKAGE_KEYGUARD_DEFAULT
            val keyguardString = resources.getString(configKeyguard)
            if(!keyguardString.contains("/")) return PACKAGE_KEYGUARD_DEFAULT
            val component = ComponentName.unflattenFromString(keyguardString)
            return component?.packageName ?: PACKAGE_KEYGUARD_DEFAULT
        }

        val PACKAGE_KEYGUARD = findKeyguard()
    }

    private val singles = module {
        single { BlurProvider.getBlurProvider(resources) }
        single { createGson() }
        single { createOkHttpClient() }
        single { createMarkwon() }
        single { SmartspaceManager(get()) }
        single<AppWidgetRepository>(createdAtStart = true) { AppWidgetRepositoryImpl(get(), get(), get(), get(), get()) }
        single<ExpandedRepository> { ExpandedRepositoryImpl(get(), get(), get(), get(), get()) }
        single<SmartspacerSettingsRepository> { SmartspacerSettingsRepositoryImpl(get()) }
        single<ShizukuServiceRepository> { ShizukuServiceRepositoryImpl(get(), get()) }
        single<SystemSmartspaceRepository>(createdAtStart = true) { SystemSmartspaceRepositoryImpl(get(), get(), get(), get(), get()) }
        single<AnalyticsRepository>(createdAtStart = true) { AnalyticsRepositoryImpl(get(), get()) }
        single<SmartspaceRepository> { SmartspaceRepositoryImpl(get(), get(), get(), get()) }
        single<TargetsRepository> { TargetsRepositoryImpl(get(), get()) }
        single<AppPredictionRepository>(createdAtStart = true) { AppPredictionRepositoryImpl(
            get(),
            get(),
            get()
        ) }
        single<AlarmRepository>(createdAtStart = true) { AlarmRepositoryImpl(get(), get(), get(), get()) }
        single<NotificationRepository>(createdAtStart = true) { NotificationRepositoryImpl(get(), get(), get(), get()) }
        single<WallpaperRepository> { WallpaperRepositoryImpl(get()) }
        single<PackageRepository> { PackageRepositoryImpl(get()) }
        single<ContainerNavigation> { ContainerNavigationImpl() }
        single<SetupNavigation> { SetupNavigationImpl() }
        single<RootNavigation> { RootNavigationImpl() }
        single<ConfigurationNavigation> { ConfigurationNavigationImpl() }
        single<WidgetOptionsNavigation> { WidgetOptionsNavigationImpl() }
        single<ExpandedNavigation> { ExpandedNavigationImpl() }
        single<DatabaseRepository> { DatabaseRepositoryImpl(get()) }
        single<RequirementsRepository> { RequirementsRepositoryImpl(get(), get()) }
        single<MediaRepository>(createdAtStart = true) { MediaRepositoryImpl(get()) }
        single<GeofenceRepository>(createdAtStart = true) { GeofenceRepositoryImpl(
            get(),
            get(),
            get()
        ) }
        single { SmartspacerDatabase.getDatabase(get()) }
        single<WidgetRepository> { WidgetRepositoryImpl(get(), get()) }
        single<GrantRepository>(createdAtStart = true) { GrantRepositoryImpl(get()) }
        single<AccessibilityRepository> { AccessibilityRepositoryImpl() }
        single<GoogleWeatherRepository> { GoogleWeatherRepositoryImpl() }
        single<AtAGlanceRepository> { AtAGlanceRepositoryImpl() }
        single<CompatibilityRepository>(createdAtStart = true) { CompatibilityRepositoryImpl(get(), get(), get(), get(), get()) }
        single<OemSmartspacerRepository>(createdAtStart = true) { OemSmartspacerRepositoryImpl(get(), get(), get(), get(), get()) }
        single<DataRepository>(createdAtStart = true) { DataRepositoryImpl(get(), get(), get()) }
        single<BroadcastRepository>(createdAtStart = true) { BroadcastRepositoryImpl(get(), get()) }
        single<SearchRepository> { SearchRepositoryImpl(get(), get(), get(), get(), get()) }
        single<BatteryOptimisationRepository> { BatteryOptimisationRepositoryImpl(get()) }
        single<PluginRepository> { PluginRepositoryImpl(get(), get(), get(), get()) }
        single<DownloadRepository> { DownloadRepositoryImpl(get()) }
        single<UpdateRepository> { UpdateRepositoryImpl(get()) }
        single<BackupRepository> { BackupRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
        single<DigitalWellbeingRepository> { DigitalWellbeingRepositoryImpl(get()) }
        single<CalendarRepository>(createdAtStart = true) { CalendarRepositoryImpl(get(), get()) }
        single<GmailRepository>(createdAtStart = true) { GmailRepositoryImpl(get(), get()) }
        single<SmsRepository>(createdAtStart = true) { SmsRepositoryImpl(get()) }
        single<CallsRepository>(createdAtStart = true) { CallsRepositoryImpl(get()) }
        single<RecentTasksRepository>(createdAtStart = true) { RecentTasksRepositoryImpl(get(), get()) }
        single<WiFiRepository>(createdAtStart = true) { WiFiRepositoryImpl(get(), get()) }
        single<BluetoothRepository>(createdAtStart = true) { BluetoothRepositoryImpl(get()) }
        single<FlashlightRepository>(createdAtStart = true) { FlashlightRepositoryImpl(get(), get()) }
    }

    private val viewModels = module {
        viewModel<MainActivityViewModel> { MainActivityViewModelImpl(get(), get(), it.get()) }
        viewModel<ContainerViewModel> { ContainerViewModelImpl(get(), get(), get(), get()) }
        viewModel<TargetsViewModel> { TargetsViewModelImpl(get(), get(), get(), get(), get(), get()) }
        viewModel<ComplicationsViewModel> { ComplicationsViewModelImpl(get(), get(), get(), get(), get(), get()) }
        viewModel<TargetsAddViewModel> { TargetsAddViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
        viewModel<ComplicationsAddViewModel> { ComplicationsAddViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
        viewModel<TargetEditViewModel> { TargetEditViewModelImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel<ComplicationEditViewModel> { ComplicationEditViewModelImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel<TargetsRequirementsViewModel> { TargetsRequirementsViewModelImpl(get()) }
        viewModel<TargetsRequirementsPageViewModel> { TargetsRequirementsPageViewModelImpl(get(), get(), get(), get()) }
        viewModel<TargetsRequirementsAddViewModel> { TargetsRequirementsAddViewModelImpl(get(), get(), get(), get()) }
        viewModel<ComplicationsRequirementsViewModel> { ComplicationsRequirementsViewModelImpl(get()) }
        viewModel<ComplicationsRequirementsPageViewModel> { ComplicationsRequirementsPageViewModelImpl(get(), get(), get(), get()) }
        viewModel<ComplicationsRequirementsAddViewModel> { ComplicationsRequirementsAddViewModelImpl(get(), get(), get(), get()) }
        viewModel<TimeDateConfigurationViewModel> { TimeDateConfigurationViewModelImpl(get(), get()) }
        viewModel<GeofenceRequirementConfigurationViewModel> { GeofenceRequirementConfigurationViewModelImpl(get(), get(), get(), get()) }
        viewModel<GeofenceRequirementConfigurationNameViewModel> { GeofenceRequirementConfigurationNameViewModelImpl() }
        viewModel<AppPredictionRequirementConfigurationViewModel> { AppPredictionRequirementConfigurationViewModelImpl(get(), get()) }
        viewModel<SettingsViewModel> { SettingsViewModelImpl(get(), get(), get()) }
        viewModel { SettingsHideSensitiveViewModel(get()) }
        viewModel<SetupLandingViewModel> { SetupLandingViewModelImpl(get()) }
        viewModel<SetupAnalyticsViewModel> { SetupAnalyticsViewModelImpl(get(), get()) }
        viewModel<SetupDecisionViewModel> { SetupDecisionViewModelImpl(get(), get(), get(), get()) }
        viewModel<SetupNotificationsViewModel> { SetupNotificationsViewModelImpl(get(), get(), get()) }
        viewModel<SetupTargetInfoViewModel> { SetupTargetInfoViewModelImpl(get(), get()) }
        viewModel<SetupTargetsViewModel> { SetupTargetsViewModelImpl(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        ) }
        viewModel<SetupComplicationInfoViewModel> { SetupComplicationInfoViewModelImpl(get()) }
        viewModel<SetupComplicationsViewModel> { SetupComplicationsViewModelImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel<SetupRequirementsInfoViewModel> { SetupRequirementsInfoViewModelImpl(get()) }
        viewModel<SetupWidgetViewModel> { SetupWidgetViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<SetupExpandedSmartspaceViewModel> { SetupExpandedSmartspaceViewModelImpl(get(), get()) }
        viewModel<SetupPluginsViewModel> { SetupPluginsViewModelImpl(get()) }
        viewModel<SetupCompleteViewModel> { SetupCompleteViewModelImpl(get(), get()) }
        viewModel<EnhancedModeViewModel> { EnhancedModeViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<EnhancedModeRequestViewModel> { EnhancedModeRequestViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<NativeModeViewModel> { NativeModeViewModelImpl(get(), get(), get(), get(), get(), get()) }
        viewModel<NativeModeSettingsViewModel> { NativeModeSettingsViewModelImpl(get(), get(), get()) }
        viewModel { NativeModePageLimitViewModel(get()) }
        viewModel<DonateViewModel> { DonateViewModelImpl(get(), get()) }
        viewModel<ExpandedViewModel> { ExpandedViewModelImpl(
            get(),
            get(),
            get(),
            get(),
            get()
        ) }
        viewModel<ExpandedSettingsViewModel> { ExpandedSettingsViewModelImpl(get(), get(), get(), get()) }
        viewModel<ExpandedSettingsSearchProviderViewModel> { ExpandedSettingsSearchProviderViewModelImpl(get(), get()) }
        viewModel { ExpandedHomeOpenModeSettingsViewModel(get()) }
        viewModel { ExpandedLockOpenModeSettingsViewModel(get()) }
        viewModel<SettingsOemSmartspaceViewModel> { SettingsOemSmartspaceViewModelImpl(get(), get(), get(), get(), get(), get()) }
        viewModel<MusicConfigurationViewModel> { MusicConfigurationViewModelImpl(get()) }
        viewModel<ExpandedAddWidgetBottomSheetViewModel> { ExpandedAddWidgetBottomSheetViewModelImpl(get(), get(), get(), get()) }
        viewModel<ExpandedRearrangeViewModel> { ExpandedRearrangeViewModelImpl(
            get(),
            get(),
            get(),
            get(),
            get()
        ) }
        viewModel<ExpandedWidgetOptionsBottomSheetViewModel> { ExpandedWidgetOptionsBottomSheetViewModelImpl(get(), get(), get()) }
        viewModel<SettingsBatteryOptimisationViewModel> { SettingsBatteryOptimisationViewModelImpl(
            get(),
            get(),
            get()
        ) }
        viewModel<SetupBatteryOptimisationViewModel> { SetupBatteryOptimisationViewModelImpl(
            get(),
            get(),
            get()
        ) }
        viewModel<PluginRepositoryViewModel> { PluginRepositoryViewModelImpl(get(), get()) }
        viewModel<PluginDetailsViewModel> { PluginDetailsViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<PluginRepositorySettingsViewModel> { PluginRepositorySettingsViewModelImpl(get(), get()) }
        viewModel<PluginRepositorySettingsUrlBottomSheetViewModel> { PluginRepositorySettingsUrlBottomSheetViewModelImpl(get(), get()) }
        viewModel<UpdateViewModel> { UpdateViewModelImpl(get(), get(), get()) }
        viewModel<PermissionsViewModel> { PermissionsViewModelImpl(get(), get()) }
        viewModel<BackupRestoreViewModel> { BackupRestoreViewModelImpl(get()) }
        viewModel<BackupViewModel> { BackupViewModelImpl(get(), get()) }
        viewModel<RestoreViewModel> { RestoreViewModelImpl(get(), get())}
        viewModel<ContributorsViewModel> { ContributorsViewModelImpl(get()) }
        viewModel<RestoreTargetsViewModel> { RestoreTargetsViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
        viewModel<RestoreComplicationsViewModel> { RestoreComplicationsViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
        viewModel<RestoreRequirementsViewModel> { RestoreRequirementsViewModelImpl(get(), get(), get(), get()) }
        viewModel<RestoreWidgetsViewModel> { RestoreWidgetsViewModelImpl(get(), get()) }
        viewModel<RestoreSettingsViewModel> { RestoreSettingsViewModelImpl(get(), get()) }
        viewModel<CalendarTargetConfigurationViewModel> { CalendarTargetConfigurationViewModelImpl(get(), get(), get()) }
        viewModel<DefaultTargetConfigurationViewModel> { DefaultTargetConfigurationViewModelImpl(get(), get(), get()) }
        viewModel<GmailComplicationConfigurationViewModel> { GmailComplicationConfigurationViewModelImpl(get(), get(), get()) }
        viewModel<RecentTaskRequirementConfigurationViewModel> { RecentTaskRequirementConfigurationViewModelImpl(get(), get(), get()) }
        viewModel<RecentTaskRequirementConfigurationAppPickerViewModel> { RecentTaskRequirementConfigurationAppPickerViewModelImpl(get(), get(), get()) }
        viewModel<RecentTaskRequirementConfigurationLimitBottomSheetViewModel> { RecentTaskRequirementConfigurationLimitBottomSheetViewModelImpl(get(), get()) }
        viewModel<NotificationTargetConfigurationViewModel> { NotificationTargetConfigurationViewModelImpl(get(), get(), get(), get()) }
        viewModel<NotificationTargetConfigurationAppPickerViewModel> { NotificationTargetConfigurationAppPickerViewModelImpl(get(), get(), get(), get()) }
        viewModel<GreetingConfigurationViewModel> { GreetingConfigurationViewModelImpl(get(), get(), get()) }
        viewModel<GreetingConfigurationNameBottomSheetViewModel> { GreetingConfigurationNameBottomSheetViewModelImpl(get(), get(), get()) }
        viewModel<WiFiRequirementConfigurationViewModel> { WiFiRequirementConfigurationViewModelImpl(get(), get(), get()) }
        viewModel<WiFiRequirementConfigurationSSIDBottomSheetViewModel> { WiFiRequirementConfigurationSSIDBottomSheetViewModelImpl(get(), get()) }
        viewModel<WiFiRequirementConfigurationMACBottomSheetViewModel> { WiFiRequirementConfigurationMACBottomSheetViewModelImpl(get(), get()) }
        viewModel<WiFiRequirementConfigurationPickerViewModel> { WiFiRequirementConfigurationPickerViewModelImpl(get(), get(), get(), get()) }
        viewModel<BlankTargetConfigurationViewModel> { BlankTargetConfigurationViewModelImpl(get()) }
        viewModel<DisplayOverOtherAppsPermissionBottomSheetViewModel> { DisplayOverOtherAppsPermissionBottomSheetViewModelImpl(get()) }
        viewModel<WidgetOptionsMenuViewModel> { WidgetOptionsMenuViewModelImpl(get(), get()) }
        viewModel<NativeReconnectViewModel> { NativeReconnectViewModelImpl(get(), get()) }
        viewModel<DumpSmartspacerViewModel> { DumpSmartspacerViewModelImpl(get(), get()) }
        viewModel<NotificationWidgetSettingsViewModel> { NotificationWidgetSettingsViewModelImpl(get(), get(), get()) }
        viewModel<DateTargetConfigurationViewModel> { DateTargetConfigurationViewModelImpl(get(), get()) }
        viewModel<DateComplicationConfigurationViewModel> { DateComplicationConfigurationViewModelImpl(get(), get()) }
        viewModel<DateFormatPickerViewModel> { DateFormatPickerViewModelImpl(get()) }
        viewModel<DateFormatCustomViewModel> { DateFormatCustomViewModelImpl() }
        viewModel<BluetoothRequirementConfigurationViewModel> { BluetoothRequirementConfigurationViewModelImpl(get(), get(), get(), get()) }
        viewModel<FlashlightTargetConfigurationViewModel> { FlashlightTargetConfigurationViewModelImpl(get(), get()) }
        viewModel<WidgetConfigurationViewModel> { WidgetConfigurationViewModelImpl(get(), get(), get(), get(), get()) }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        HiddenApiBypass.addHiddenApiExemptions("")
        Sui.init(packageName)
        if(isSafeMode()) return
        startKoin {
            androidContext(base)
            modules(singles, viewModels)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if(isSafeMode()) return
        DynamicColors.applyToActivitiesIfAvailable(this)
        setupMonet()
    }

    private fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalTimeAdapter())
            .setExclusionStrategies(gsonExclusionStrategy)
            .create()
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    private fun createMarkwon(): Markwon {
        val typeface = ResourcesCompat.getFont(this, R.font.google_sans_text_medium)
        return Markwon.builder(this)
            .usePlugins(listOf(
                MovementMethodPlugin.create(BetterLinkMovementMethod.getInstance()),
                object: AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        typeface?.let {
                            builder.headingTypeface(it)
                            builder.headingBreakHeight(0)
                        }
                    }
                }
            )).build()
    }

    private fun setupMonet(){
        val settings = get<SmartspacerSettingsRepository>()
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor.getSync()
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

    private fun isSafeMode(): Boolean {
        return getProcessName() != packageName
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMaxSchedulerLimit(AlarmRepository.MAX_SCHEDULER_LIMIT)
            .build()

}
package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.enable

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationNotificationEnableBinding
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.service.SmartspacerNotificationListenerService
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.getNotificationListenerIntent
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastU
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.smartspacer.utils.extensions.shouldShowRequireSideload
import com.kieronquinn.app.smartspacer.utils.extensions.showAppInfo
import com.kieronquinn.app.smartspacer.utils.extensions.wasInstalledWithSession
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.android.ext.android.inject

class ConfigurationNotificationEnableFragment: BoundFragment<FragmentConfigurationNotificationEnableBinding>(FragmentConfigurationNotificationEnableBinding::inflate) {

    private val settings by inject<SmartspacerSettingsRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRestrictedCard()
        setupNotificationAccessCard()
        setupWarningCard()
    }

    private fun setupToolbar() = with(binding.configurationNotificationToolbar){
        val background = monet.getBackgroundColor(requireContext())
        val primary = monet.getPrimaryColor(requireContext())
        binding.root.setBackgroundColor(background)
        val toolbar = monet.getBackgroundColorSecondary(requireContext()) ?: primary
        requireActivity().window.statusBarColor = toolbar
        requireActivity().window.navigationBarColor = background
        setBackgroundColor(toolbar)
        whenResumed {
            onNavigationIconClicked().collect {
                requireActivity().run {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupRestrictedCard() = with(binding.configurationNotificationContent) {
        val shouldShow = !isAtLeastU() || requireContext().wasInstalledWithSession()
        configurationNotificationInfo.isVisible = shouldShow
        configurationNotificationInfo.applyBackgroundTint(monet)
        configurationNotificationInfoButton.applyMonet()
        whenResumed {
            configurationNotificationInfoButton.onClicked().collect {
                Toast.makeText(
                    requireContext(), R.string.toast_restricted_mode, Toast.LENGTH_LONG
                ).show()
                requireContext().showAppInfo()
            }
        }
    }

    private fun setupNotificationAccessCard() = with(binding.configurationNotificationContent) {
        configurationNotificationAccess.applyBackgroundTint(monet)
        configurationNotificationAccessButton.applyMonet()
        whenResumed {
            configurationNotificationAccessButton.onClicked().collect {
                startActivity(requireContext().getNotificationListenerIntent(
                    SmartspacerNotificationListenerService::class.java
                ))
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupWarningCard() = with(binding.configurationNotificationContent) {
        val shouldShow = requireContext().shouldShowRequireSideload()
        configurationNotificationAccessWarning.applyBackgroundTint(monet)
        configurationNotificationAccessWarning.isVisible = shouldShow
        configurationNotificationAccessWarningButton.isVisible = settings.enhancedMode.getSync()
        configurationNotificationAccessWarningButton.applyMonet()
        configurationNotificationAccessWarningText.run {
            text = android.text.Html.fromHtml(
                getString(R.string.restricted_mode_content_warning_notification),
                android.text.Html.FROM_HTML_MODE_LEGACY
            )
            Linkify.addLinks(this, Linkify.WEB_URLS)
            movementMethod = me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().setOnLinkClickListener { _, url ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                startActivity(intent)
                true
            }
        }
        whenResumed {
            configurationNotificationAccessWarningButton.onClicked().collect {
                grantPermission()
            }
        }
    }

    private fun grantPermission() = whenResumed {
        shizukuServiceRepository.runWithService {
            it.grantRestrictedSettings()
        }
        binding.configurationNotificationContent.configurationNotificationAccessWarningButton
            .isVisible = false
    }

}
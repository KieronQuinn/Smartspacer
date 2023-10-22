package com.kieronquinn.app.smartspacer.ui.screens.configuration.accessibility

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationAccessibilityEnableBinding
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.getAccessibilityIntent
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastU
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.smartspacer.utils.extensions.showAppInfo
import com.kieronquinn.app.smartspacer.utils.extensions.wasInstalledWithSession
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject

class ConfigurationAccessibilityEnableFragment: BoundFragment<FragmentConfigurationAccessibilityEnableBinding>(FragmentConfigurationAccessibilityEnableBinding::inflate) {

    private val settings by inject<SmartspacerSettingsRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRestrictedCard()
        setupAccessibilityAccessCard()
        setupWarningCard()
    }

    private fun setupToolbar() = with(binding.configurationAccessibilityToolbar){
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
    private fun setupRestrictedCard() {
        val shouldShow = !isAtLeastU() || requireContext().wasInstalledWithSession()
        binding.configurationAccessibilityInfo.isVisible = shouldShow
        binding.configurationAccessibilityInfo.applyBackgroundTint(monet)
        binding.configurationAccessibilityInfoButton.applyMonet()
        whenResumed {
            binding.configurationAccessibilityInfoButton.onClicked().collect {
                Toast.makeText(
                    requireContext(), R.string.toast_restricted_mode, Toast.LENGTH_LONG
                ).show()
                requireContext().showAppInfo()
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupWarningCard() {
        val shouldShow = isAtLeastU() && !requireContext().wasInstalledWithSession()
        binding.configurationAccessibilityWarning.applyBackgroundTint(monet)
        binding.configurationAccessibilityWarning.isVisible = shouldShow
        binding.configurationAccessibilityWarningButton.isVisible = settings.enhancedMode.getSync()
        binding.configurationAccessibilityWarningButton.applyMonet()
        binding.configurationAccessibilityWarningText.run {
            text = Html.fromHtml(
                getString(R.string.restricted_mode_content_warning_accessibility),
                Html.FROM_HTML_MODE_LEGACY
            )
            Linkify.addLinks(this, Linkify.WEB_URLS)
            movementMethod = BetterLinkMovementMethod.newInstance().setOnLinkClickListener { _, url ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                startActivity(intent)
                true
            }
        }
        whenResumed {
            binding.configurationAccessibilityWarningButton.onClicked().collect {
                grantPermission()
            }
        }
    }

    private fun setupAccessibilityAccessCard() {
        binding.configurationAccessibilityAccess.applyBackgroundTint(monet)
        binding.configurationAccessibilityAccessButton.applyMonet()
        whenResumed {
            binding.configurationAccessibilityAccessButton.onClicked().collect {
                startActivity(requireContext().getAccessibilityIntent(
                    SmartspacerAccessibiltyService::class.java
                ))
            }
        }
    }

    private fun grantPermission() = whenResumed {
        shizukuServiceRepository.runWithService {
            it.grantRestrictedSettings()
        }
        binding.configurationAccessibilityWarning.isVisible = false
    }

}
package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification

import android.annotation.SuppressLint
import android.app.Activity
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationTargetNotificationBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.service.SmartspacerNotificationListenerService
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.NotificationTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.allowBackground
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.getNotificationListenerIntent
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.shouldShowRequireSideload
import com.kieronquinn.app.smartspacer.utils.extensions.showAppInfo
import com.kieronquinn.app.smartspacer.utils.extensions.wasInstalledWithSession
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotificationTargetConfigurationFragment: BoundFragment<FragmentConfigurationTargetNotificationBinding>(FragmentConfigurationTargetNotificationBinding::inflate), BackAvailable {

    override val backIcon = R.drawable.ic_close

    private val viewModel by viewModel<NotificationTargetConfigurationViewModel>()
    private val settings by inject<SmartspacerSettingsRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    private val pendingCallback = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        //No-op, handled by resume
    }

    private val adapter by lazy {
        Adapter()
    }

    private val companionManager by lazy {
        requireContext().getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCompanionButton()
        setupReadMoreButton()
        setupRestrictedCard()
        setupNotificationAccessCard()
        setupWarningCard()
        setupState()
        setupInsets()
        setupRecyclerView()
        val id = requireActivity().intent.getStringExtra(EXTRA_SMARTSPACER_ID) ?: return
        viewModel.setupWithId(id)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupRecyclerView() = with(binding.configurationTargetNotificationSettings) {
        settingsBaseRecyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NotificationTargetConfigurationFragment.adapter
            updatePadding(top = resources.getDimension(R.dimen.margin_16).toInt())
        }
    }

    private fun setupInsets() = with(binding) {
        val standardPadding = resources.getDimension(R.dimen.margin_16).toInt()
        configurationTargetNotificationSettings.settingsBaseRecyclerView
            .onApplyInsets { view, insets ->
                val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                view.updatePadding(bottom = standardPadding + bottom)
            }
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
        when(state){
            is State.Loading -> {
                configurationTargetNotificationSettings.settingsBaseLoading.isVisible = true
                configurationTargetNotificationSettings.settingsBaseRecyclerView.isVisible = false
                configurationTargetNotificationAssociation.root.isVisible = false
                configurationTargetNotificationEnable.root.isVisible = false
            }
            is State.GrantNotificationAccess -> {
                configurationTargetNotificationEnable.root.isVisible = true
                configurationTargetNotificationSettings.settingsBaseLoading.isVisible = false
                configurationTargetNotificationSettings.settingsBaseRecyclerView.isVisible = false
                configurationTargetNotificationAssociation.root.isVisible = false
            }
            is State.GrantAssociation -> {
                configurationTargetNotificationAssociation.root.isVisible = true
                configurationTargetNotificationSettings.settingsBaseLoading.isVisible = false
                configurationTargetNotificationSettings.settingsBaseRecyclerView.isVisible = false
                configurationTargetNotificationEnable.root.isVisible = false
            }
            is State.Settings -> {
                //Anything beyond this point counts as a success and should reload the target
                requireActivity().setResult(Activity.RESULT_OK)
                configurationTargetNotificationAssociation.root.isVisible = false
                configurationTargetNotificationSettings.settingsBaseLoading.isVisible = false
                configurationTargetNotificationSettings.settingsBaseRecyclerView.isVisible = true
                configurationTargetNotificationEnable.root.isVisible = false
                adapter.update(
                    state.loadItems(),
                    configurationTargetNotificationSettings.settingsBaseRecyclerView
                )
            }
        }
    }

    private fun State.Settings.loadItems(): List<BaseSettingsItem> {
        val app = GenericSettingsItem.Setting(
            getString(R.string.target_notification_configuration_app_title),
            selectedAppLabel ?: getString(R.string.target_notification_configuration_app_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_target_notifications),
            onClick = viewModel::onAppClicked
        )
        val channels = if(availableChannels.isNotEmpty()){
            val header = GenericSettingsItem.Header(
                getString(R.string.target_notification_channels)
            )
            listOf(header) + availableChannels.toList()
                .sortedWith(compareBy(nullsLast()) { it.first?.name?.toString()?.lowercase() })
                .flatMap {
                listOf(
                    GenericSettingsItem.Header(
                        it.first?.name ?: getString(R.string.target_notification_channels_no_group)
                    )
                ) + it.second.sortedBy { channel ->
                    channel.name?.toString()?.lowercase()
                }.map { channel ->
                    GenericSettingsItem.SwitchSetting(
                        options.channels.contains(channel.id),
                        channel.name ?: "",
                        channel.description ?: "",
                        null
                    ) { enabled ->
                        viewModel.onChannelChanged(channel.id, enabled)
                    }
                }
            }
        }else if(options.packageName != null){
            listOf(
                GenericSettingsItem.Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                    getString(R.string.target_notification_channels_empty)
                )
            )
        }else emptyList()
        val settings = listOf(
            GenericSettingsItem.Header(getString(R.string.target_notification_configuration_options)),
            GenericSettingsItem.SwitchSetting(
                options.trimNewLines,
                getString(R.string.target_notification_configuration_trim_new_lines_title),
                getString(R.string.target_notification_configuration_trim_new_lines_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_target_notification_new_line),
                onChanged = viewModel::onTrimNewLinesChanged
            )
        ).takeIf { options.packageName != null } ?: emptyList()
        return listOf(app) + channels + settings
    }

    private fun requestPairing() {
        companionManager.associate(
            AssociationRequest.Builder().build(),
            object: CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pendingCallback.launch(
                            IntentSenderRequest.Builder(intentSender).build(),
                            ActivityOptionsCompat.makeBasic().allowBackground()
                        )
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onDeviceFound(intentSender: IntentSender) {
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        pendingCallback.launch(
                            IntentSenderRequest.Builder(intentSender).build(),
                            ActivityOptionsCompat.makeBasic().allowBackground()
                        )
                    }
                }

                override fun onFailure(error: CharSequence?) {
                    onError()
                }
            },
            null
        )
    }

    private fun onError() {
        Toast.makeText(
            requireContext(),
            getString(R.string.configuration_notification_association_toast),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setupCompanionButton() = with(binding.configurationTargetNotificationAssociation) {
        configurationNotificationAssociationButton.run {
            applyMonet()
            whenResumed {
                onClicked().collect {
                    requestPairing()
                }
            }
        }
    }

    private fun setupReadMoreButton() = with(binding.configurationTargetNotificationAssociation) {
        configurationNotificationAssociationReadMore.run {
            setTextColor(monet.getAccentColor(context))
            whenResumed {
                onClicked().collect {
                    viewModel.onReadMoreClicked()
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupRestrictedCard() = with(binding.configurationTargetNotificationEnable) {
        val shouldShow = !requireContext().wasInstalledWithSession()
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

    private fun setupNotificationAccessCard() = with(binding.configurationTargetNotificationEnable) {
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
    private fun setupWarningCard() = with(binding.configurationTargetNotificationEnable) {
        val shouldShow = requireContext().shouldShowRequireSideload()
        configurationNotificationAccessWarning.applyBackgroundTint(monet)
        configurationNotificationAccessWarning.isVisible = shouldShow
        configurationNotificationAccessWarningButton.isVisible = settings.enhancedMode.getSync()
        configurationNotificationAccessWarningButton.applyMonet()
        configurationNotificationAccessWarningText.run {
            text = Html.fromHtml(
                getString(R.string.restricted_mode_content_warning_notification),
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
            configurationNotificationAccessWarningButton.onClicked().collect {
                grantPermission()
            }
        }
    }

    private fun grantPermission() = whenResumed {
        shizukuServiceRepository.runWithService {
            it.grantRestrictedSettings()
        }
        binding.configurationTargetNotificationEnable.configurationNotificationAccessWarningButton
            .isVisible = false
    }

    inner class Adapter: BaseSettingsAdapter(
        binding.configurationTargetNotificationSettings.settingsBaseRecyclerView,
        emptyList()
    )

}
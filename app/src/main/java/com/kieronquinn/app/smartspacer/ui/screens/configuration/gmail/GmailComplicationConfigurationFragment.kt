package com.kieronquinn.app.smartspacer.ui.screens.configuration.gmail

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Header
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.SwitchSetting
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.gmail.GmailComplicationConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.utils.gmail.GmailContract
import org.koin.androidx.viewmodel.ext.android.viewModel

class GmailComplicationConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        private const val ACCOUNT_TYPE_GOOGLE = "com.google"
    }

    private val viewModel by viewModel<GmailComplicationConfigurationViewModel>()

    private val requestPermissionContract = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if(it){
            viewModel.setHasGrantedPermission(it)
        }else{
            viewModel.launchPermissionSettings(requireContext())
        }
    }

    private val accountPickerContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode == Activity.RESULT_OK) {
            val accountName = it.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                ?: return@registerForActivityResult
            viewModel.onAccountSelected(accountName)
        }
    }

    override val adapter by lazy {
        Adapter()
    }

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        val id = requireActivity().intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
            ?: return
        viewModel.setupWithId(id)
        requestPermissionContract.launch(GmailContract.PERMISSION)
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
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.loadItems(), binding.settingsBaseRecyclerView)
                if(state.settings.accountName != null && state.settings.enabledLabels.isNotEmpty()) {
                    requireActivity().setResult(Activity.RESULT_OK)
                }
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        val labels = if(labels.isNotEmpty()){
            listOf(
                Header(
                    getString(R.string.complication_gmail_settings_title_labels)
                ),
                *labels.map {
                    SwitchSetting(
                        settings.enabledLabels.contains(it.canonicalName),
                        it.name,
                        "",
                        null,
                    ) { enabled ->
                        viewModel.onLabelChanged(it, enabled)
                    }
                }.toTypedArray()
            )
        }else emptyList()
        return listOf(
            Setting(
                getString(R.string.complication_gmail_settings_account_title),
                settings.accountName
                    ?: getString(R.string.complication_gmail_settings_account_unselected),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_complication_gmail_configuration_account
                )
            ) {
                onAccountClicked(settings.accountName)
            }
        ) + labels
    }

    private fun onAccountClicked(selectedName: String?) {
        val intent = AccountManager.newChooseAccountIntent(
            selectedName?.let { Account(it, ACCOUNT_TYPE_GOOGLE) },
            null,
            arrayOf(ACCOUNT_TYPE_GOOGLE),
            null,
            null,
            null,
            null
        )
        accountPickerContract.launch(intent)
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}
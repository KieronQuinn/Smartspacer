package com.kieronquinn.app.smartspacer.ui.screens.backuprestore

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreFragment: BaseSettingsFragment(), BackAvailable, HideBottomNavigation {

    private val backupLauncher = registerForActivityResult(CreateDocument("*/*")) {
        if(it == null) return@registerForActivityResult
        viewModel.onBackupSelected(it)
    }

    private val restoreLauncher = registerForActivityResult(OpenDocument()) {
        if(it == null) return@registerForActivityResult
        viewModel.onRestoreSelected(it)
    }

    private val viewModel by viewModel<BackupRestoreViewModel>()

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        Adapter(items)
    }

    private val items by lazy {
        listOf(
            Setting(
                getString(R.string.backup_title),
                getString(R.string.backup_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_backup_restore_backup),
                onClick = ::onBackupClicked
            ),
            Setting(
                getString(R.string.restore_title),
                getString(R.string.restore_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_backup_restore_restore),
                onClick = ::onRestoreClicked
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsBaseRecyclerView.isVisible = true
        binding.settingsBaseLoading.isVisible = false
    }

    private fun onBackupClicked() {
        viewModel.onBackupClicked(backupLauncher)
    }

    private fun onRestoreClicked() {
        viewModel.onRestoreClicked(restoreLauncher)
    }

    inner class Adapter(override var items: List<BaseSettingsItem>):
        BaseSettingsAdapter(binding.settingsBaseRecyclerView, items)

}
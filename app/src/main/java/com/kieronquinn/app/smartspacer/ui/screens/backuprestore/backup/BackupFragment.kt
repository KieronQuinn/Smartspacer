package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.backup

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentBackupBinding
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupFragment: BoundFragment<FragmentBackupBinding>(FragmentBackupBinding::inflate), BackAvailable, HideBottomNavigation {

    private val viewModel by viewModel<BackupViewModel>()
    private val navArgs by navArgs<BackupFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupMonet()
        setupClose()
        viewModel.setupWithUri(navArgs.uri)
    }

    private fun setupMonet() {
        binding.backupRestoreBackupProgress.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.backupRestoreBackupClose.setTextColor(accent)
        binding.backupRestoreBackupClose.overrideRippleColor(accent)
    }

    private fun setupClose() = with(binding.backupRestoreBackupClose) {
        whenResumed {
            onClicked().collect {
                viewModel.onCloseClicked()
            }
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

    private fun handleState(state: SmartspacerBackupProgress) {
        when(state){
            is SmartspacerBackupProgress.CreatingBackup -> {
                binding.backupRestoreBackupTitle.setText(R.string.backup_creating_backup)
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupProgress.isIndeterminate = true
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupDesc.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
            }
            is SmartspacerBackupProgress.CreatingTargetsBackup -> {
                binding.backupRestoreBackupTitle
                    .setText(R.string.backup_creating_targets_backup)
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupProgress.isIndeterminate = false
                binding.backupRestoreBackupProgress.progress = state.progress
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupDesc.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
            }
            is SmartspacerBackupProgress.CreatingComplicationsBackup -> {
                binding.backupRestoreBackupTitle
                    .setText(R.string.backup_creating_complications_backup)
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupProgress.isIndeterminate = false
                binding.backupRestoreBackupProgress.progress = state.progress
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupDesc.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
            }
            is SmartspacerBackupProgress.CreatingRequirementsBackup -> {
                binding.backupRestoreBackupTitle
                    .setText(R.string.backup_creating_requirements_backup)
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupProgress.isIndeterminate = false
                binding.backupRestoreBackupProgress.progress = state.progress
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupDesc.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
            }
            is SmartspacerBackupProgress.CreatingCustomWidgetsBackup -> {
                binding.backupRestoreBackupTitle
                    .setText(R.string.backup_creating_widgets_backup)
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupProgress.isIndeterminate = true
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupDesc.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
            }
            is SmartspacerBackupProgress.CreatingSettingsBackup -> {
                binding.backupRestoreBackupTitle
                    .setText(R.string.backup_creating_settings_backup)
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupProgress.isIndeterminate = true
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupDesc.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
            }
            is SmartspacerBackupProgress.WritingFile -> {
                binding.backupRestoreBackupTitle.setText(R.string.backup_creating_backup)
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupProgress.isIndeterminate = true
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupDesc.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
            }
            is SmartspacerBackupProgress.Finished -> {
                binding.backupRestoreBackupTitle.setText(R.string.backup_created)
                binding.backupRestoreBackupDesc.isVisible = !state.filename.isNullOrEmpty()
                binding.backupRestoreBackupDesc.text = state.filename
                binding.backupRestoreBackupIcon.setImageResource(R.drawable.ic_check_circle)
                binding.backupRestoreBackupIcon.isVisible = true
                binding.backupRestoreBackupClose.isVisible = true
                binding.backupRestoreBackupProgress.isVisible = false
            }
            is SmartspacerBackupProgress.Error -> {
                binding.backupRestoreBackupTitle.setText(R.string.backup_created)
                binding.backupRestoreBackupDesc.text = getString(state.reason.description)
                binding.backupRestoreBackupIcon.setImageResource(R.drawable.ic_cross_circle)
                binding.backupRestoreBackupIcon.isVisible = true
                binding.backupRestoreBackupClose.isVisible = true
                binding.backupRestoreBackupProgress.isVisible = false
            }
        }
    }

}
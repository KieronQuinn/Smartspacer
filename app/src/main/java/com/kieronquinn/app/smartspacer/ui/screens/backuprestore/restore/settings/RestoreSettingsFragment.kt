package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.databinding.FragmentRestoreSettingsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreSettingsFragment: BoundFragment<FragmentRestoreSettingsBinding>(FragmentRestoreSettingsBinding::inflate), BackAvailable, LockCollapsed, HideBottomNavigation {

    private val viewModel by viewModel<RestoreSettingsViewModel>()
    private val navArgs by navArgs<RestoreSettingsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        viewModel.setupWithConfig(navArgs.config)
    }

    private fun setupMonet() {
        binding.backupRestoreSettingsProgress.applyMonet()
    }

}
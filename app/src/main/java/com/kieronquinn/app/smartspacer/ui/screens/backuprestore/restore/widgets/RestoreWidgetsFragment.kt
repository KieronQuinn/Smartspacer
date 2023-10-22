package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.widgets

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.databinding.FragmentRestoreWidgetsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreWidgetsFragment: BoundFragment<FragmentRestoreWidgetsBinding>(FragmentRestoreWidgetsBinding::inflate), BackAvailable, LockCollapsed, HideBottomNavigation {

    private val viewModel by viewModel<RestoreWidgetsViewModel>()
    private val navArgs by navArgs<RestoreWidgetsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        viewModel.setupWithConfig(navArgs.config)
    }

    private fun setupMonet() {
        binding.backupRestoreWidgetsProgress.applyMonet()
    }

}
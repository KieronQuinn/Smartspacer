package com.kieronquinn.app.smartspacer.ui.screens.setup.notifications

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupNotificationsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupNotificationsFragment: BoundFragment<FragmentSetupNotificationsBinding>(FragmentSetupNotificationsBinding::inflate), BackAvailable, ProvidesBack, LockCollapsed {

    private val viewModel by viewModel<SetupNotificationsViewModel>()

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onPermissionResult(requireContext(), it)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupScrollable()
        setupMonet()
        setupGrant()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermission(requireContext())
    }

    private fun setupMonet() {
        val accentColor = monet.getAccentColor(requireContext())
        binding.setupNotificationsRequest.run {
            iconTint = ColorStateList.valueOf(accentColor)
            setTextColor(accentColor)
        }
    }

    private fun setupScrollable() = with(binding.setupNotificationsScrollable) {
        isNestedScrollingEnabled = false
        onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
        }
    }

    private fun setupGrant() = whenResumed {
        binding.setupNotificationsRequest.onClicked().collect {
            viewModel.onGrantClicked(notificationPermissionRequest)
        }
    }

    override fun onBackPressed() = viewModel.onBackPressed()

}
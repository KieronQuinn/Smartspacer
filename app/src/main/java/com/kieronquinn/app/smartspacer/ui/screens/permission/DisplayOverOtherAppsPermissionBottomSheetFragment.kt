package com.kieronquinn.app.smartspacer.ui.screens.permission

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentPermissionDisplayOverOtherAppsBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class DisplayOverOtherAppsPermissionBottomSheetFragment: BaseBottomSheetFragment<FragmentPermissionDisplayOverOtherAppsBinding>(FragmentPermissionDisplayOverOtherAppsBinding::inflate) {

    private val viewModel by viewModel<DisplayOverOtherAppsPermissionBottomSheetViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInsets()
        setupGrant()
        setupLater()
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.permissionDisplayOverOtherAppsPositive.setTextColor(accent)
        binding.permissionDisplayOverOtherAppsNegative.setTextColor(accent)
    }

    private fun setupInsets() = with(binding.root) {
        val padding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            updatePadding(bottom = bottomInset + padding)
        }
    }

    private fun setupGrant() = with(binding.permissionDisplayOverOtherAppsPositive) {
        whenResumed {
            onClicked().collect {
                viewModel.onGrantClicked()
            }
        }
    }

    private fun setupLater() = with(binding.permissionDisplayOverOtherAppsNegative) {
        whenResumed {
            onClicked().collect {
                viewModel.onDismissClicked()
            }
        }
    }

}
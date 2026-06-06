package com.kieronquinn.app.smartspacer.ui.screens.shizuku

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentShizukuOutdatedBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.getShizukuInstallIntent
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class ShizukuOutdatedBottomSheetFragment: BaseBottomSheetFragment<FragmentShizukuOutdatedBinding>(FragmentShizukuOutdatedBinding::inflate) {

    private val viewModel by viewModel<ShizukuOutdatedBottomSheetViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInsets()
        setupPositive()
        setupNegative()
        setupNeutral()
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.shizukuOutdatedPositive.setTextColor(accent)
        binding.shizukuOutdatedNegative.setTextColor(accent)
        binding.shizukuOutdatedNeutral.setTextColor(accent)
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

    private fun setupPositive() = with(binding.shizukuOutdatedPositive) {
        whenResumed {
            onClicked().collect {
                dismiss()
                startActivity(getShizukuInstallIntent())
            }
        }
    }

    private fun setupNegative() = with(binding.shizukuOutdatedNegative) {
        whenResumed {
            onClicked().collect {
                dismiss()
            }
        }
    }

    private fun setupNeutral() = with(binding.shizukuOutdatedNeutral) {
        whenResumed {
            onClicked().collect {
                viewModel.onIgnoreClicked()
                dismiss()
            }
        }
    }

}
package com.kieronquinn.app.smartspacer.ui.screens.donate

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentDonateBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class DonateFragment: BoundFragment<FragmentDonateBinding>(FragmentDonateBinding::inflate), BackAvailable, HideBottomNavigation {

    private val viewModel by viewModel<DonateViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInsets()
        setupPayPal()
        setupDisable()
        setupDonationPrompt()
    }

    private fun setupMonet() {
        (binding.donateDisable as Button).applyMonet()
        binding.donatePaypal.applyMonet()
    }

    private fun setupInsets() {
        val bottomPadding = resources.getDimensionPixelSize(R.dimen.margin_16)
        binding.root.onApplyInsets { view, insets ->
            view.updatePadding(bottom = insets.getInsets(systemBars()).bottom + bottomPadding)
        }
    }

    private fun setupPayPal() = with(binding.donatePaypal) {
        whenResumed {
            onClicked().collect {
                viewModel.onPayPalClicked()
            }
        }
    }

    private fun setupDisable() = with(binding.donateDisable) {
        whenResumed {
            onClicked().collect {
                viewModel.onDisableClicked()
            }
        }
    }

    private fun setupDonationPrompt() {
        handleDonationPrompt(viewModel.isDonatePromptEnabled.value)
        whenResumed {
            viewModel.isDonatePromptEnabled.collect {
                handleDonationPrompt(it)
            }
        }
    }

    private fun handleDonationPrompt(enabled: Boolean) {
        binding.donateDisable.isVisible = enabled
        binding.donateHideInfo.isVisible = enabled
    }

}
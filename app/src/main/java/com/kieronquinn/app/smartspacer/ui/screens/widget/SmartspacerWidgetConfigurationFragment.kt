package com.kieronquinn.app.smartspacer.ui.screens.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSmartspacerWidgetConfigurationBinding
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.ui.activities.permission.accessibility.AccessibilityPermissionActivity
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.widget.SmartspacerWidgetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class SmartspacerWidgetConfigurationFragment: BoundFragment<FragmentSmartspacerWidgetConfigurationBinding>(FragmentSmartspacerWidgetConfigurationBinding::inflate), BackAvailable, LockCollapsed {

    private val viewModel by viewModel<SmartspacerWidgetConfigurationViewModel>()

    override val backIcon = R.drawable.ic_close

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAppWidget()
        setupAccessibilityCard()
        setupFab()
        setupScroll()
        setupAccessibilityButton()
        setupHomeCard()
        setupLockCard()
        setupPageSingleCard()
        setupPageControlsCard()
        setupPageNoControlsCard()
        setupColourAutomaticCard()
        setupColourWhiteCard()
        setupColourBlackCard()
        setupHomeButton()
        setupHomeRadioButton()
        setupLockButton()
        setupLockRadioButton()
        setupColourAutomaticButton()
        setupColourAutomaticRadioButton()
        setupColourWhiteButton()
        setupColourWhiteRadioButton()
        setupColourBlackButton()
        setupColourBlackRadioButton()
        setupPageSingleButton()
        setupPageSingleRadioButton()
        setupPageControlsButton()
        setupPageControlsRadioButton()
        setupPageNoControlsButton()
        setupPageNoControlsRadioButton()
        setupState()
        setupClose()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupAppWidget() {
        val intent = requireActivity().intent
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val calling = requireActivity().callingPackage ?: ""
        if(appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
            requireActivity().setResult(Activity.RESULT_CANCELED)
            requireActivity().finish()
            return
        }
        val appWidget = AppWidget(
            appWidgetId,
            calling,
            UiSurface.HOMESCREEN,
            TintColour.AUTOMATIC,
            multiPage = true,
            showControls = true
        )
        viewModel.setupWithAppWidget(appWidget)
    }

    private fun setupScroll() = with(binding.smartspacerWidgetConfigurationScroll) {
        isNestedScrollingEnabled = false
        val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
        onApplyInsets { view, insets ->
            view.updatePadding(bottom =
                insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom + fabMargin
            )
        }
    }

    private fun setupFab() = with(binding.smartspacerWidgetConfigurationApply) {
        backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val bottomMargin = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                val bottom =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom + bottomMargin
                updateMargins(bottom = bottom)
            }
        }
        whenResumed {
            onClicked().collect {
                viewModel.onApplyClicked()
            }
        }
    }

    private fun setupAccessibilityCard() = with(binding.smartspacerWidgetConfigurationAccessibility) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupAccessibilityButton() = with(binding.smartspacerWidgetConfigurationAccessibilityButton) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                onEnableAccessibilityClicked()
            }
        }
    }

    private fun setupHomeCard() = with(binding.smartspacerWidgetConfigurationTypeHomeCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupLockCard() = with(binding.smartspacerWidgetConfigurationTypeLockCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupPageSingleCard() = with(binding.smartspacerWidgetConfigurationPageSingleCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupPageControlsCard() = with(binding.smartspacerWidgetConfigurationPageControlsCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupPageNoControlsCard() = with(binding.smartspacerWidgetConfigurationPageNoControlsCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupColourAutomaticCard() = with(binding.smartspacerWidgetConfigurationColourAutomaticCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupColourBlackCard() = with(binding.smartspacerWidgetConfigurationColourBlackCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupColourWhiteCard() = with(binding.smartspacerWidgetConfigurationColourWhiteCard) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun setupHomeButton() = with(binding.smartspacerWidgetConfigurationTypeHome) {
        whenResumed {
            onClicked().collect {
                viewModel.onHomeClicked()
            }
        }
    }

    private fun setupHomeRadioButton() = with(binding.smartspacerWidgetConfigurationTypeHomeRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onHomeClicked()
            }
        }
    }

    private fun setupLockButton() = with(binding.smartspacerWidgetConfigurationTypeLock) {
        whenResumed {
            onClicked().collect {
                viewModel.onLockClicked()
            }
        }
    }

    private fun setupLockRadioButton() = with(binding.smartspacerWidgetConfigurationTypeLockRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onLockClicked()
            }
        }
    }

    private fun setupColourAutomaticButton() = with(binding.smartspacerWidgetConfigurationColourAutomatic) {
        whenResumed {
            onClicked().collect {
                viewModel.onColourAutomaticClicked()
            }
        }
    }

    private fun setupColourAutomaticRadioButton() = with(binding.smartspacerWidgetConfigurationColourAutomaticRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onColourAutomaticClicked()
            }
        }
    }

    private fun setupColourWhiteButton() = with(binding.smartspacerWidgetConfigurationColourWhite) {
        whenResumed {
            onClicked().collect {
                viewModel.onColourWhiteClicked()
            }
        }
    }

    private fun setupColourWhiteRadioButton() = with(binding.smartspacerWidgetConfigurationColourWhiteRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onColourWhiteClicked()
            }
        }
    }

    private fun setupColourBlackButton() = with(binding.smartspacerWidgetConfigurationColourBlack) {
        whenResumed {
            onClicked().collect {
                viewModel.onColourBlackClicked()
            }
        }
    }

    private fun setupColourBlackRadioButton() = with(binding.smartspacerWidgetConfigurationColourBlackRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onColourBlackClicked()
            }
        }
    }

    private fun setupPageSingleButton() = with(binding.smartspacerWidgetConfigurationPageSingle) {
        whenResumed {
            onClicked().collect {
                viewModel.onPageSingleClicked()
            }
        }
    }

    private fun setupPageSingleRadioButton() = with(binding.smartspacerWidgetConfigurationPageSingleRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onPageSingleClicked()
            }
        }
    }

    private fun setupPageControlsButton() = with(binding.smartspacerWidgetConfigurationPageControls) {
        whenResumed {
            onClicked().collect {
                viewModel.onPageControlsClicked()
            }
        }
    }

    private fun setupPageControlsRadioButton() = with(binding.smartspacerWidgetConfigurationPageControlsRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onPageControlsClicked()
            }
        }
    }

    private fun setupPageNoControlsButton() = with(binding.smartspacerWidgetConfigurationPageNoControls) {
        whenResumed {
            onClicked().collect {
                viewModel.onPageNoControlsClicked()
            }
        }
    }

    private fun setupPageNoControlsRadioButton() = with(binding.smartspacerWidgetConfigurationPageNoControlsRadio) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onPageNoControlsClicked()
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

    private fun handleState(state: State) {
        when (state) {
            is State.Loading -> {
                binding.smartspacerWidgetConfigurationScroll.isVisible = false
                binding.smartspacerWidgetConfigurationApply.isVisible = false
            }
            is State.Close -> {
                requireActivity().setResult(Activity.RESULT_CANCELED)
                requireActivity().finish()
            }
            is State.Loaded -> {
                binding.smartspacerWidgetConfigurationAccessibility.isVisible =
                    !state.isAccessibilityServiceEnabled
                binding.smartspacerWidgetConfigurationScroll.isVisible = true
                binding.smartspacerWidgetConfigurationApply.isVisible =
                    state.isAccessibilityServiceEnabled
                binding.smartspacerWidgetConfigurationTypeHomeRadio.isChecked =
                    state.appWidget.surface == UiSurface.HOMESCREEN
                binding.smartspacerWidgetConfigurationTypeLockRadio.isChecked =
                    state.appWidget.surface == UiSurface.LOCKSCREEN
                binding.smartspacerWidgetConfigurationColourAutomaticRadio.isChecked =
                    state.appWidget.tintColour == TintColour.AUTOMATIC
                binding.smartspacerWidgetConfigurationColourWhiteRadio.isChecked =
                    state.appWidget.tintColour == TintColour.WHITE
                binding.smartspacerWidgetConfigurationColourBlackRadio.isChecked =
                    state.appWidget.tintColour == TintColour.BLACK
                binding.smartspacerWidgetConfigurationPageSingleRadio.isChecked =
                    !state.appWidget.multiPage
                binding.smartspacerWidgetConfigurationPageControlsRadio.isChecked =
                    state.appWidget.multiPage && state.appWidget.showControls
                binding.smartspacerWidgetConfigurationPageNoControlsRadio.isChecked =
                    state.appWidget.multiPage && !state.appWidget.showControls
                if(state.isPossiblyLockScreen){
                    binding.smartspacerWidgetConfigurationTypeLockCard.alpha = 1f
                    binding.smartspacerWidgetConfigurationTypeLockContent
                        .setText(R.string.widget_configuration_type_lock_content)
                    binding.smartspacerWidgetConfigurationTypeLockRadio.isEnabled = true
                    binding.smartspacerWidgetConfigurationTypeLock.isClickable = true
                }else{
                    binding.smartspacerWidgetConfigurationTypeLockCard.alpha = 0.5f
                    binding.smartspacerWidgetConfigurationTypeLockContent
                        .setText(R.string.widget_configuration_type_lock_content_disabled)
                    binding.smartspacerWidgetConfigurationTypeLockRadio.isEnabled = false
                    binding.smartspacerWidgetConfigurationTypeLock.isClickable = false
                }
            }
        }
    }

    private fun setupClose() {
        whenResumed {
            viewModel.closeBus.collect {
                requireActivity().let {
                    it.setResult(Activity.RESULT_OK)
                    it.finish()
                }
            }
        }
    }

    private fun onEnableAccessibilityClicked() {
        startActivity(Intent(requireContext(), AccessibilityPermissionActivity::class.java))
    }

}
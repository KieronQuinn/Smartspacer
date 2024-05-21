package com.kieronquinn.app.smartspacer.ui.screens.setup.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupWidgetFragmentBinding
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.setup.widget.SetupWidgetViewModel.Companion.INTENT_PIN_WIDGET
import com.kieronquinn.app.smartspacer.ui.screens.setup.widget.SetupWidgetViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.isServiceRunning
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.registerReceiverCompat
import com.kieronquinn.app.smartspacer.utils.extensions.unregisterReceiverCompat
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupWidgetFragment: BoundFragment<FragmentSetupWidgetFragmentBinding>(FragmentSetupWidgetFragmentBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupWidgetViewModel>()
    private val pinCallback = WidgetPinCallback()
    private var shouldShowAccessibility = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCard()
        setupMonet()
        setupControls()
        setupLockscreen()
        setupState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().registerReceiverCompat(
            pinCallback,
            IntentFilter(INTENT_PIN_WIDGET)
        )
    }

    override fun onDestroy() {
        requireContext().unregisterReceiverCompat(pinCallback)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if(shouldShowAccessibility && !isServiceRunning()){
            viewModel.launchAccessibility(requireContext())
        }
        val loadedState = viewModel.state.value as? State.Loaded
        if(loadedState?.hasClickedWidget == true && isServiceRunning()) {
            //Assume widget has been added, and move on
            viewModel.onNextClicked()
            return
        }
        shouldShowAccessibility = false
    }

    private fun setupMonet() {
        binding.setupWidgetLoading.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        binding.setupWidgetLoadingInner.loadingProgress.applyMonet()
    }

    private fun setupCard() = with(binding.setupWidgetCard) {
        applyBackgroundTint(monet)
        whenResumed {
            onClicked().collect {
                viewModel.onWidgetClicked()
            }
        }
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupWidgetControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupWidgetControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupWidgetControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupWidgetControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

    private fun setupLockscreen() = with(binding.setupAddWidgetLock) {
        text = Html.fromHtml(
            getString(R.string.setup_add_widget_lock),
            Html.FROM_HTML_MODE_LEGACY
        )
        Linkify.addLinks(this, Linkify.WEB_URLS)
        movementMethod = BetterLinkMovementMethod.newInstance().setOnLinkClickListener { _, url ->
            viewModel.openUrl(url)
            true
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
        when(state){
            is State.Loading -> {
                binding.setupWidgetLoading.isVisible = true
                binding.setupWidgetCard.isVisible = false
                binding.setupAddWidgetLock.isVisible = false
                binding.setupWidgetControls.isVisible = false
            }
            is State.Loaded -> {
                if(state.shouldSkip){
                    viewModel.onNextClicked(true)
                    return
                }
                binding.setupWidgetLoading.isVisible = false
                binding.setupWidgetCard.isVisible = true
                binding.setupAddWidgetLock.isVisible = state.shouldShowLockscreenInfo
                val buttonText = if(state.hasClickedWidget){
                    R.string.setup_targets_controls_next
                }else{
                    R.string.setup_targets_controls_skip
                }
                binding.setupWidgetControlsNext.setText(buttonText)
                binding.setupWidgetControls.isVisible = true
            }
        }
    }

    private fun isServiceRunning(): Boolean {
        return requireContext().isServiceRunning(SmartspacerAccessibiltyService::class.java)
    }

    private inner class WidgetPinCallback: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            viewModel.onWidgetAdded(context)
            shouldShowAccessibility =
                !context.isServiceRunning(SmartspacerAccessibiltyService::class.java)
        }

    }

}
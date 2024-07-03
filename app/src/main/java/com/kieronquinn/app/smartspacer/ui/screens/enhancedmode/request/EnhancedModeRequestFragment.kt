package com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.request

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.databinding.FragmentEnhancedModeRequestBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.request.EnhancedModeRequestViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class EnhancedModeRequestFragment: BoundFragment<FragmentEnhancedModeRequestBinding>(FragmentEnhancedModeRequestBinding::inflate), BackAvailable {

    private val viewModel by viewModel<EnhancedModeRequestViewModel>()
    private val args by navArgs<EnhancedModeRequestFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoading()
        setupShizuku()
        setupSui()
        setupShizukuError()
        setupState()
    }

    private fun setupLoading() {
        binding.enhancedModeRequestProgress.applyMonet()
    }

    private fun setupShizuku() {
        binding.enhancedModeRequestShizuku.applyBackgroundTint(monet)
        whenResumed {
            binding.enhancedModeRequestShizukuButton.onClicked().collect {
                viewModel.onGetShizukuClicked(requireContext(), args.isSetup)
            }
        }
    }

    private fun setupSui() {
        binding.enhancedModeRequestSui.applyBackgroundTint(monet)
        whenResumed {
            binding.enhancedModeRequestSuiButton.onClicked().collect {
                viewModel.onGetSuiClicked(args.isSetup)
            }
        }
    }

    private fun setupShizukuError() = with(binding.enhancedModeRequestShizukuErrorOpen) {
        applyMonet()
        viewLifecycleOwner.lifecycleScope.launch {
            onClicked().collect {
                viewModel.onOpenShizukuClicked(args.isSetup)
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
        when(state){
            is State.Requesting -> {
                binding.enhancedModeRequest.isVisible = true
                binding.enhancedModeShizukuError.isVisible = false
                binding.enhancedModeRequestInfo.isVisible = false
            }
            is State.Info -> {
                binding.enhancedModeRequest.isVisible = false
                binding.enhancedModeShizukuError.isVisible = false
                binding.enhancedModeRequestInfo.isVisible = true
            }
            is State.StartShizuku -> {
                binding.enhancedModeRequest.isVisible = false
                binding.enhancedModeShizukuError.isVisible = true
                binding.enhancedModeRequestInfo.isVisible = false
            }
            is State.Result -> {
                if(state.granted){
                    viewModel.onGranted(args.isSetup)
                }else{
                    viewModel.onDenied(args.isSetup)
                }
            }
        }
    }

}
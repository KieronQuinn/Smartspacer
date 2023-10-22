package com.kieronquinn.app.smartspacer.ui.screens.setup.decision

import android.os.Bundle
import android.view.View
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupDecisionBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.setup.decision.SetupDecisionViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupDecisionFragment: BoundFragment<FragmentSetupDecisionBinding>(FragmentSetupDecisionBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupDecisionViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupState()
    }

    private fun setupMonet() = with(binding.setupDecisionLoading) {
        loadingProgress.applyMonet()
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
        if(state is State.Loaded){
            viewModel.navigateTo(state.directions)
        }
    }

}
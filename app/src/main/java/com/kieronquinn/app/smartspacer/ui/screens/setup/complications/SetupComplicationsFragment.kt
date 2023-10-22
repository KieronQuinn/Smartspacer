package com.kieronquinn.app.smartspacer.ui.screens.setup.complications

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupComplicationsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsFragment
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupComplicationsFragment: BaseAddComplicationsFragment<FragmentSetupComplicationsBinding>(FragmentSetupComplicationsBinding::inflate), BackAvailable {

    private val adapter by lazy {
        SetupComplicationsAdapter(
            binding.setupComplicationsRecyclerview,
            emptyList(),
            viewModel::onComplicationClicked
        )
    }

    override val viewModel by viewModel<SetupComplicationsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupLoading()
        setupControls()
        setupState()
    }

    override fun onDismiss(complication: Item.Complication) {
        //We use "dismiss" to add a complication here since we don't actually close
        viewModel.addComplication(complication)
    }

    private fun setupRecyclerView() = with(binding.setupComplicationsRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@SetupComplicationsFragment.adapter
    }

    private fun setupLoading() = with(binding.setupComplicationsLoading) {
        loadingProgress.applyMonet()
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupComplicationsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupComplicationsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupComplicationsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupComplicationsControlsNext.onClicked().collect {
                viewModel.onNextClicked()
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
            is State.Loading -> {
                binding.setupComplicationsLoading.root.isVisible = true
                binding.setupComplicationsRecyclerview.isVisible = false
                binding.setupComplicationsControls.isVisible = false
            }
            is State.Loaded -> {
                binding.setupComplicationsLoading.root.isVisible = false
                binding.setupComplicationsRecyclerview.isVisible = true
                binding.setupComplicationsControls.isVisible = true
                val items = state.items.filterIsInstance<Item.Complication>()
                if(items.isEmpty()){
                    //Nothing can be added on this page, so move to the next one
                    viewModel.onNextClicked()
                    return
                }
                val nextRes = if(items.any { it.isAdded }) {
                    R.string.setup_complications_controls_next
                }else{
                    R.string.setup_complications_controls_skip
                }
                binding.setupComplicationsControlsNext.text = getString(nextRes)
                adapter.items = items
                adapter.notifyDataSetChanged()
            }
        }
    }

}
package com.kieronquinn.app.smartspacer.ui.screens.setup.targets

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupTargetsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsFragment
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupTargetsFragment: BaseAddTargetsFragment<FragmentSetupTargetsBinding>(FragmentSetupTargetsBinding::inflate), BackAvailable {

    private val adapter by lazy {
        SetupTargetsAdapter(
            binding.setupTargetsRecyclerview,
            emptyList(),
            viewModel::onTargetClicked
        )
    }

    override val viewModel by viewModel<SetupTargetsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupLoading()
        setupControls()
        setupState()
    }

    override fun onDismiss(target: Item.Target) {
        //We use "dismiss" to add a target here since we don't actually close
        viewModel.addTarget(target)
    }

    private fun setupRecyclerView() = with(binding.setupTargetsRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@SetupTargetsFragment.adapter
    }

    private fun setupLoading() = with(binding.setupTargetsLoading) {
        loadingProgress.applyMonet()
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupTargetsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupTargetsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupTargetsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupTargetsControlsNext.onClicked().collect {
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
                binding.setupTargetsLoading.root.isVisible = true
                binding.setupTargetsRecyclerview.isVisible = false
                binding.setupTargetsControls.isVisible = false
            }
            is State.Loaded -> {
                binding.setupTargetsLoading.root.isVisible = false
                binding.setupTargetsRecyclerview.isVisible = true
                binding.setupTargetsControls.isVisible = true
                val items = state.items.filterIsInstance<Item.Target>()
                if(items.isEmpty()){
                    //Nothing can be added on this page, so move to the next one
                    viewModel.onNextClicked()
                    return
                }
                val nextRes = if(items.any { it.isAdded }) {
                    R.string.setup_targets_controls_next
                }else{
                    R.string.setup_targets_controls_skip
                }
                binding.setupTargetsControlsNext.text = getString(nextRes)
                adapter.items = items
                adapter.notifyDataSetChanged()
            }
        }
    }

}
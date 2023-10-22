package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.targets

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRestoreTargetsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsFragment
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreTargetsFragment: BaseAddTargetsFragment<FragmentRestoreTargetsBinding>(
    FragmentRestoreTargetsBinding::inflate
), BackAvailable, HideBottomNavigation, LockCollapsed {

    private val adapter by lazy {
        RestoreTargetsAdapter(
            binding.restoreTargetsRecyclerview,
            emptyList(),
            viewModel::onTargetClicked
        )
    }

    override val viewModel by viewModel<RestoreTargetsViewModel>()

    private val navArgs by navArgs<RestoreTargetsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupControls()
        setupMonet()
        setupState()
        viewModel.setupWithConfig(navArgs.config)
    }

    private fun setupMonet() {
        binding.restoreTargetsLoading.loadingProgress.applyMonet()
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.restoreTargetsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.restoreTargetsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.restoreTargetsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.restoreTargetsControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

    private fun setupRecyclerView() = with(binding.restoreTargetsRecyclerview) {
        isNestedScrollingEnabled = false
        adapter = this@RestoreTargetsFragment.adapter
        layoutManager = LinearLayoutManager(context)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: BaseAddTargetsViewModel.State) {
        when(state){
            is BaseAddTargetsViewModel.State.Loading -> {
                binding.restoreTargetsLoading.root.isVisible = true
                binding.restoreTargetsRecyclerview.isVisible = false
                binding.restoreTargetsControls.isVisible = false
            }
            is BaseAddTargetsViewModel.State.Loaded -> {
                binding.restoreTargetsLoading.root.isVisible = false
                binding.restoreTargetsControls.isVisible = true
                val items = state.items.filterIsInstance<Item.Target>()
                binding.restoreTargetsRecyclerview.isVisible = items.isNotEmpty()
                binding.restoreTargetsEmpty.isVisible = items.isEmpty()
                adapter.items = items
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDismiss(target: Item.Target) {
        viewModel.onAdded(target)
    }

}
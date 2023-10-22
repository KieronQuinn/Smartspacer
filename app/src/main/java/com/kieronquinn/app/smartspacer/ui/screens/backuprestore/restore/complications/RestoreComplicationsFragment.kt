package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complications

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRestoreComplicationsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsFragment
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreComplicationsFragment: BaseAddComplicationsFragment<FragmentRestoreComplicationsBinding>(
    FragmentRestoreComplicationsBinding::inflate
), BackAvailable, HideBottomNavigation, LockCollapsed {

    private val adapter by lazy {
        RestoreComplicationsAdapter(
            binding.restoreComplicationsRecyclerview,
            emptyList(),
            viewModel::onComplicationClicked
        )
    }

    override val viewModel by viewModel<RestoreComplicationsViewModel>()

    private val navArgs by navArgs<RestoreComplicationsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupControls()
        setupMonet()
        setupState()
        viewModel.setupWithConfig(navArgs.config)
    }

    private fun setupMonet() {
        binding.restoreComplicationsLoading.loadingProgress.applyMonet()
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.restoreComplicationsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.restoreComplicationsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.restoreComplicationsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.restoreComplicationsControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

    private fun setupRecyclerView() = with(binding.restoreComplicationsRecyclerview) {
        isNestedScrollingEnabled = false
        adapter = this@RestoreComplicationsFragment.adapter
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

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.restoreComplicationsLoading.root.isVisible = true
                binding.restoreComplicationsRecyclerview.isVisible = false
                binding.restoreComplicationsControls.isVisible = false
            }
            is State.Loaded -> {
                binding.restoreComplicationsLoading.root.isVisible = false
                binding.restoreComplicationsControls.isVisible = true
                val items = state.items.filterIsInstance<Item.Complication>()
                binding.restoreComplicationsRecyclerview.isVisible = items.isNotEmpty()
                binding.restoreComplicationsEmpty.isVisible = items.isEmpty()
                adapter.items = items
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDismiss(complication: Item.Complication) {
        viewModel.onAdded(complication)
    }

}
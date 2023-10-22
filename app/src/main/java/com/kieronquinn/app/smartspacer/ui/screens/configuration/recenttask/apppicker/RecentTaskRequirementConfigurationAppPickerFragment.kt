package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.apppicker

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRecentTaskRequirementConfigurationPickerBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker.NotificationTargetConfigurationAppPickerAdapter
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.apppicker.RecentTaskRequirementConfigurationAppPickerViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecentTaskRequirementConfigurationAppPickerFragment: BoundFragment<FragmentRecentTaskRequirementConfigurationPickerBinding>(FragmentRecentTaskRequirementConfigurationPickerBinding::inflate), BackAvailable {

    private val viewModel by viewModel<RecentTaskRequirementConfigurationAppPickerViewModel>()
    private val navArgs by navArgs<RecentTaskRequirementConfigurationAppPickerFragmentArgs>()

    private val adapter by lazy {
        NotificationTargetConfigurationAppPickerAdapter(
            binding.recentTaskRequirementConfigurationAppPickerRecyclerView,
            emptyList()
        ) {
            viewModel.onAppClicked(navArgs.id, it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearch()
        setupSearchClear()
        setupState()
        setupMonet()
        setupRecyclerView()
    }

    private fun setupRecyclerView() = with(binding.recentTaskRequirementConfigurationAppPickerRecyclerView) {
        adapter = this@RecentTaskRequirementConfigurationAppPickerFragment.adapter
        layoutManager = LinearLayoutManager(context)
        val bottomPadding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottomPadding + bottomInset)
        }
    }

    private fun setupMonet() {
        binding.recentTaskRequirementConfigurationAppPickerLoading.loadingProgress.applyMonet()
        binding.recentTaskRequirementConfigurationAppPickerSearch.searchBox.applyMonet()
        binding.recentTaskRequirementConfigurationAppPickerSearch.searchBox.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColorSecondary(requireContext())
                ?: monet.getBackgroundColor(requireContext()))
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.recentTaskRequirementConfigurationAppPickerSearch.searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.recentTaskRequirementConfigurationAppPickerSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.recentTaskRequirementConfigurationAppPickerSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.recentTaskRequirementConfigurationAppPickerSearch.searchBox.run {
            this.text?.let {
                it.clear()
                it.append(text)
            } ?: setText(text)
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
                binding.recentTaskRequirementConfigurationAppPickerLoading.root.isVisible = true
                binding.recentTaskRequirementConfigurationAppPickerRecyclerView.isVisible = false
                binding.recentTaskRequirementConfigurationAppPickerSearch.root.isVisible = false
            }
            is State.Loaded -> {
                binding.recentTaskRequirementConfigurationAppPickerLoading.root.isVisible = false
                binding.recentTaskRequirementConfigurationAppPickerRecyclerView.isVisible = true
                binding.recentTaskRequirementConfigurationAppPickerSearch.root.isVisible = true
                adapter.items = state.apps
                adapter.notifyDataSetChanged()
            }
        }
    }

}
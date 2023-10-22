package com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentAppPredictionRequirementConfigurationBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction.AppPredictionRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AppPredictionRequirementConfigurationFragment: BoundFragment<FragmentAppPredictionRequirementConfigurationBinding>(FragmentAppPredictionRequirementConfigurationBinding::inflate), BackAvailable {

    private val viewModel by viewModel<AppPredictionRequirementConfigurationViewModel>()

    private val id by lazy {
        requireActivity().intent.getStringExtra(com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID)!!
    }

    private val adapter by lazy {
        AppPredictionRequirementConfigurationAdapter(
            binding.appPredictionRequirementConfigurationRecyclerView,
            emptyList()
        ) {
            viewModel.onAppClicked(id, it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClose()
        setupSearch()
        setupSearchClear()
        setupState()
        setupMonet()
        setupRecyclerView()
    }

    private fun setupClose() {
        whenResumed {
            viewModel.closeBus.collect {
                requireActivity().run {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun setupRecyclerView() = with(binding.appPredictionRequirementConfigurationRecyclerView) {
        adapter = this@AppPredictionRequirementConfigurationFragment.adapter
        layoutManager = LinearLayoutManager(context)
        val bottomPadding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottomPadding + bottomInset)
        }
    }

    private fun setupMonet() {
        binding.appPredictionRequirementConfigurationLoading.loadingProgress.applyMonet()
        binding.appPredictionRequirementConfigurationSearch.searchBox.applyMonet()
        binding.appPredictionRequirementConfigurationSearch.searchBox.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColorSecondary(requireContext())
                ?: monet.getBackgroundColor(requireContext()))
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.appPredictionRequirementConfigurationSearch.searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.appPredictionRequirementConfigurationSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.appPredictionRequirementConfigurationSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.appPredictionRequirementConfigurationSearch.searchBox.run {
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
                binding.appPredictionRequirementConfigurationLoading.root.isVisible = true
                binding.appPredictionRequirementConfigurationRecyclerView.isVisible = false
                binding.appPredictionRequirementConfigurationSearch.root.isVisible = false
            }
            is State.Loaded -> {
                binding.appPredictionRequirementConfigurationLoading.root.isVisible = false
                binding.appPredictionRequirementConfigurationRecyclerView.isVisible = true
                binding.appPredictionRequirementConfigurationSearch.root.isVisible = true
                adapter.items = state.apps
                adapter.notifyDataSetChanged()
            }
        }
    }

}
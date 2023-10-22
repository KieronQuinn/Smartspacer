package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationTargetNotificationAppPickerBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker.NotificationTargetConfigurationAppPickerViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotificationTargetConfigurationAppPickerFragment: BoundFragment<FragmentConfigurationTargetNotificationAppPickerBinding>(FragmentConfigurationTargetNotificationAppPickerBinding::inflate), BackAvailable {

    private val viewModel by viewModel<NotificationTargetConfigurationAppPickerViewModel>()
    private val navArgs by navArgs<NotificationTargetConfigurationAppPickerFragmentArgs>()

    private val adapter by lazy {
        NotificationTargetConfigurationAppPickerAdapter(
            binding.notificationTargetConfigurationAppPickerRecyclerView,
            emptyList()
        ) {
            viewModel.onAppSelected(navArgs.id, it.packageName)
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

    private fun setupRecyclerView() = with(binding.notificationTargetConfigurationAppPickerRecyclerView) {
        adapter = this@NotificationTargetConfigurationAppPickerFragment.adapter
        layoutManager = LinearLayoutManager(context)
        val bottomPadding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottomPadding + bottomInset)
        }
    }

    private fun setupMonet() {
        binding.notificationTargetConfigurationAppPickerLoading.loadingProgress.applyMonet()
        binding.notificationTargetConfigurationAppPickerSearch.searchBox.applyMonet()
        binding.notificationTargetConfigurationAppPickerSearch.searchBox.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColorSecondary(requireContext())
                ?: monet.getBackgroundColor(requireContext()))
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.notificationTargetConfigurationAppPickerSearch.searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.notificationTargetConfigurationAppPickerSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.notificationTargetConfigurationAppPickerSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.notificationTargetConfigurationAppPickerSearch.searchBox.run {
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
                binding.notificationTargetConfigurationAppPickerLoading.root.isVisible = true
                binding.notificationTargetConfigurationAppPickerRecyclerView.isVisible = false
                binding.notificationTargetConfigurationAppPickerSearch.root.isVisible = false
            }
            is State.Loaded -> {
                binding.notificationTargetConfigurationAppPickerLoading.root.isVisible = false
                binding.notificationTargetConfigurationAppPickerRecyclerView.isVisible = true
                binding.notificationTargetConfigurationAppPickerSearch.root.isVisible = true
                adapter.items = state.apps
                adapter.notifyDataSetChanged()
            }
        }
    }

}
package com.kieronquinn.app.smartspacer.ui.screens.complications.add

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentComplicationsAddBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsFragment
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMarginShort
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ComplicationsAddFragment: BaseAddComplicationsFragment<FragmentComplicationsAddBinding>(FragmentComplicationsAddBinding::inflate), BackAvailable {

    companion object {
        const val REQUEST_KEY_COMPLICATIONS_ADD = "complications_add"
        const val RESULT_KEY_PACKAGE_NAME = "package_name"
        const val RESULT_KEY_AUTHORITY = "authority"
        const val RESULT_KEY_ID = "id"
        const val RESULT_KEY_NOTIFICATION_AUTHORITY = "notification_authority"
        const val RESULT_KEY_BROADCAST_AUTHORITY = "broadcast_authority"
    }
    
    override val viewModel by viewModel<ComplicationsAddViewModel>()

    private val adapter by lazy {
        ComplicationsAddAdapter(
            binding.complicationsAddRecyclerview,
            viewModel::onExpandClicked,
            viewModel::onComplicationClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupMonet()
        setupEmpty()
        setupSearch()
        setupSearchClear()
    }

    private fun setupMonet() {
        binding.loading.loadingProgress.applyMonet()
        binding.includeSearch.searchBox.applyMonet()
        binding.includeSearch.searchBox.backgroundTintList = ColorStateList.valueOf(
            monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(
                requireContext()
            )
        )
    }

    private fun setupRecyclerView() = with(binding.complicationsAddRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@ComplicationsAddFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
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
                binding.loading.root.isVisible = true
                binding.complicationsAddEmpty.isVisible = false
                binding.complicationsAddLoaded.isVisible = false
            }
            is State.Loaded -> {
                binding.loading.root.isVisible = false
                binding.complicationsAddEmpty.isVisible = state.items.isEmpty()
                binding.complicationsAddLoaded.isVisible = true
                adapter.submitList(state.items)
            }
        }
    }

    private fun setupEmpty() = with(binding.complicationsAddEmptyLabel) {
        applyBottomNavigationMarginShort(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.includeSearch.searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.includeSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.includeSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.includeSearch.searchBox.run {
            this.text?.let {
                it.clear()
                it.append(text)
            } ?: setText(text)
        }
    }

    override fun onDestroyView() {
        binding.complicationsAddRecyclerview.adapter = null
        super.onDestroyView()
    }

    override fun onDismiss(complication: BaseAddComplicationsViewModel.Item.Complication) {
        setFragmentResult(
            REQUEST_KEY_COMPLICATIONS_ADD, bundleOf(
                RESULT_KEY_AUTHORITY to complication.authority,
                RESULT_KEY_PACKAGE_NAME to complication.packageName,
                RESULT_KEY_ID to complication.id,
                RESULT_KEY_NOTIFICATION_AUTHORITY to complication.notificationAuthority,
                RESULT_KEY_BROADCAST_AUTHORITY to complication.broadcastAuthority
            )
        )
        viewModel.dismiss()
    }

}
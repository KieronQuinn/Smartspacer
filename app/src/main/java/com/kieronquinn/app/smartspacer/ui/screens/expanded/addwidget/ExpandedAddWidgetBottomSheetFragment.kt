package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentExpandedBottomSheetAddWidgetBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.allowBackground
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.screenOff
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExpandedAddWidgetBottomSheetFragment: BaseBottomSheetFragment<FragmentExpandedBottomSheetAddWidgetBinding>(FragmentExpandedBottomSheetAddWidgetBinding::inflate) {

    private val viewModel by viewModel<ExpandedAddWidgetBottomSheetViewModel>()

    private val widgetBindResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onWidgetBindResult(it.resultCode == Activity.RESULT_OK)
    }

    private val widgetConfigureResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.onWidgetConfigureResult(it.resultCode == Activity.RESULT_OK)
    }

    override val fullScreen = true

    private val adapter by lazy {
        ExpandedAddWidgetBottomSheetAdapter(
            binding.addWidgetRecyclerView,
            ::getAvailableWidth,
            viewModel::onExpandClicked,
            viewModel::onWidgetClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInsets()
        setupRecyclerView()
        setupState()
        setupSearch()
        setupSearchClear()
        setupAddState()
        setupClose()
        setupCloseWhenLocked()
    }

    private fun setupMonet() {
        binding.addWidgetLoading.loadingProgress.applyMonet()
        binding.addWidgetSearch.searchBox.applyMonet()
        binding.addWidgetSearch.searchBox.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColorSecondary(requireContext())
                ?: monet.getBackgroundColor(requireContext()))
    }

    private fun setupInsets() {
        val standardPadding = resources.getDimensionPixelSize(R.dimen.margin_16)
        binding.addWidgetRecyclerView.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            view.updatePadding(bottom =  + standardPadding + bottom)
        }
    }

    private fun setupRecyclerView() = with(binding.addWidgetRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@ExpandedAddWidgetBottomSheetFragment.adapter
    }

    private fun setupClose() = whenResumed {
        viewModel.exitBus.collect {
            if(it) {
                dismiss()
            }
        }
    }

    private fun setupCloseWhenLocked() {
        whenResumed {
            requireContext().screenOff().collect {
                if(it) dismiss()
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
                binding.addWidgetLoading.root.isVisible = true
                binding.addWidgetRecyclerView.isVisible = false
                binding.addWidgetEmpty.isVisible = false
            }
            is State.Loaded -> {
                binding.addWidgetLoading.root.isVisible = false
                binding.addWidgetRecyclerView.isVisible = true
                binding.addWidgetEmpty.isVisible = state.items.isEmpty()
                adapter.submitList(state.items)
            }
        }
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.addWidgetSearch.searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.addWidgetSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.addWidgetSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.addWidgetSearch.searchBox.run {
            this.text?.let {
                it.clear()
                it.append(text)
            } ?: setText(text)
        }
    }

    private fun setupAddState() = whenResumed {
        viewModel.addState.drop(1).collect {
            handleAddState(it)
        }
    }

    private fun getAvailableWidth(): Int {
        //Width of the recycler view - standard item padding - widget padding
        return binding.addWidgetRecyclerView.measuredWidth -
                (resources.getDimensionPixelSize(R.dimen.margin_16) * 4)
    }

    private fun handleAddState(state: AddState) {
        when(state) {
            is AddState.BindWidget -> {
                if(viewModel.bindAppWidgetIfAllowed(state.info.provider, state.id)){
                    viewModel.onWidgetBindResult(true)
                }else{
                    val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, state.id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, state.info.provider)
                    }
                    widgetBindResult.launch(bindIntent)
                }
            }
            is AddState.ConfigureWidget -> {
                val configureIntentSender = viewModel.createConfigIntentSender(state.id)
                widgetConfigureResult.launch(
                    IntentSenderRequest.Builder(configureIntentSender).build(),
                    ActivityOptionsCompat.makeBasic().allowBackground()
                )
            }
            is AddState.WidgetError -> {
                Toast.makeText(
                    requireContext(), R.string.complications_add_incompatible_widget, Toast.LENGTH_LONG
                ).show()
            }
            is AddState.Dismiss -> {
                dismiss()
            }
            is AddState.Idle -> {
                //No-op
            }
        }
    }

}
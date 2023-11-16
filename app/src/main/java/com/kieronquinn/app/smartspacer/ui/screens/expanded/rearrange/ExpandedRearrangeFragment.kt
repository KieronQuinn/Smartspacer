package com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange

import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.databinding.FragmentExpandedRearrangeBinding
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.model.doodle.DoodleImage
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.expanded.BaseExpandedAdapter
import com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange.ExpandedRearrangeViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExpandedRearrangeFragment: BoundFragment<FragmentExpandedRearrangeBinding>(FragmentExpandedRearrangeBinding::inflate),
    SmartspacerBasePageView.SmartspaceTargetInteractionListener, BaseExpandedAdapter.ExpandedAdapterListener {

    private val viewModel by viewModel<ExpandedRearrangeViewModel>()

    private val adapter by lazy {
        ExpandedRearrangeAdapter(
            emptyList(),
            binding.expandedRearrangeRecyclerView,
            this
        )
    }

    private val itemTouchHelper by lazy {
        ItemTouchHelper(ExpandedRearrangeItemTouchHelperCallback(viewModel, adapter))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupState()
        setupClose()
        WindowCompat.getInsetsController(requireActivity().window, requireView())
            .isAppearanceLightStatusBars = !requireContext().isDarkMode
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun setupToolbar() = with(binding.expandedRearrangeToolbar) {
        whenResumed {
            onNavigationIconClicked().collect {
                viewModel.onBackPressed()
            }
        }
        val background = monet.getBackgroundColorSecondary(context)
            ?: monet.getBackgroundColor(requireContext())
        setBackgroundColor(background)
        onApplyInsets { view, insets ->
            view.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
        }
    }

    private fun setupRecyclerView() = with(binding.expandedRearrangeRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@ExpandedRearrangeFragment.adapter
        val bottomPadding = resources.getDimensionPixelSize(R.dimen.margin_16)
        onApplyInsets { view, insets ->
            view.updatePadding(bottom =
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom + bottomPadding
            )
        }
        itemTouchHelper.attachToRecyclerView(this)
    }

    private fun setupClose() = whenResumed {
        viewModel.exitBus.collect {
            if(it) {
                viewModel.onBackPressed()
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
                binding.expandedRearrangeLoading.root.isVisible = true
                binding.expandedRearrangeRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.expandedRearrangeLoading.root.isVisible = false
                binding.expandedRearrangeRecyclerView.isVisible = true
                adapter.items = state.items
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onConfigureWidgetClicked(
        info: AppWidgetProviderInfo,
        id: String?,
        config: ExpandedRepository.CustomExpandedAppWidgetConfig?
    ) {
        //No-op
    }

    override fun onShortcutClicked(shortcut: Shortcut) {
        //No-op
    }

    override fun onAppShortcutClicked(appShortcut: AppShortcut) {
        //No-op
    }

    override fun onAddWidgetClicked() {
        //No-op
    }

    override fun onCustomWidgetLongClicked(view: View, widget: Item.Widget) {
        //No-op
    }

    override fun onWidgetLongClicked(viewHolder: ViewHolder, appWidgetId: Int?) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onWidgetDeleteClicked(widget: Item.RemovedWidget) {
        //No-op
    }

    override fun onInteraction(target: SmartspaceTarget, actionId: String?) {
        //No-op
    }

    override fun onLongPress(target: SmartspaceTarget): Boolean {
        return false
    }

    override fun onDoodleClicked(doodleImage: DoodleImage) {
        //No-op
    }

    override fun onSearchBoxClicked(searchApp: SearchRepository.SearchApp) {
        //No-op
    }

    override fun onSearchLensClicked(searchApp: SearchRepository.SearchApp) {
        //No-op
    }

    override fun onSearchMicClicked(searchApp: SearchRepository.SearchApp) {
        //No-op
    }

}
package com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange

import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
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
import com.kieronquinn.app.smartspacer.utils.extensions.WIDGET_MIN_COLUMNS
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetColumnCount
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExpandedRearrangeFragment: BoundFragment<FragmentExpandedRearrangeBinding>(FragmentExpandedRearrangeBinding::inflate),
    SmartspacerBasePageView.SmartspaceTargetInteractionListener, BaseExpandedAdapter.ExpandedAdapterListener {

    private val viewModel by viewModel<ExpandedRearrangeViewModel>()
    private var multiColumnEnabled = true

    private val adapter by lazy {
        ExpandedRearrangeAdapter(
            emptyList(),
            binding.expandedRearrangeRecyclerView,
            this,
            ::getSpanPercent,
            ::getAvailableWidth
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
        layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            alignItems = AlignItems.CENTER
            justifyContent = JustifyContent.CENTER
            flexWrap = FlexWrap.WRAP
        }
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

    private fun getAvailableWidth(): Int {
        return binding.expandedRearrangeRecyclerView.measuredWidth - 16.dp
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.expandedRearrangeLoading.root.isVisible = true
                binding.expandedRearrangeRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                multiColumnEnabled = state.multiColumnEnabled
                binding.expandedRearrangeLoading.root.isVisible = false
                binding.expandedRearrangeRecyclerView.isVisible = true
                adapter.items = state.items
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun getSpanPercent(item: Item): Float {
        var columnCount = requireContext().getWidgetColumnCount(getAvailableWidth())
        if(!multiColumnEnabled) {
            //Prevent widgets being displayed alongside each other when multi column is disabled
            columnCount = columnCount.coerceAtMost(WIDGET_MIN_COLUMNS)
        }
        val targetBasedColumns = if(multiColumnEnabled) {
            (columnCount / WIDGET_MIN_COLUMNS.toFloat()).coerceAtLeast(1f)
        }else 1f
        val targetBasedWidth = (1f / targetBasedColumns)
        return when(item) {
            is Item.Widget -> {
                return when {
                    item.fullWidth -> targetBasedWidth
                    item.spanX != null -> {
                        item.spanX / columnCount.toFloat()
                    }
                    else -> targetBasedWidth //Unlikely to expect being full width when wide
                }
            }
            is Item.RemovedWidget -> targetBasedWidth
            else -> 1f //Not implemented on this screen
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

    override fun onCustomWidgetLongClicked(view: View, widget: Item.Widget): Boolean {
        return false
    }

    override fun onWidgetLongClicked(viewHolder: ViewHolder, appWidgetId: Int?): Boolean {
        itemTouchHelper.startDrag(viewHolder)
        return true
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
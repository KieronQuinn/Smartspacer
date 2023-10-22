package com.kieronquinn.app.smartspacer.ui.screens.targets

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentTargetsBinding
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.CanShowSnackbar
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesOverflow
import com.kieronquinn.app.smartspacer.ui.base.Root
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter.ItemHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerItemTouchHelperCallback
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModel.State
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsViewModel.TargetHolder
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddFragment.Companion.REQUEST_KEY_TARGETS_ADD
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddFragment.Companion.RESULT_KEY_AUTHORITY
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddFragment.Companion.RESULT_KEY_BROADCAST_AUTHORITY
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddFragment.Companion.RESULT_KEY_ID
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddFragment.Companion.RESULT_KEY_NOTIFICATION_AUTHORITY
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddFragment.Companion.RESULT_KEY_PACKAGE_NAME
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMarginShort
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel


class TargetsFragment: BoundFragment<FragmentTargetsBinding>(FragmentTargetsBinding::inflate), ProvidesOverflow, LockCollapsed, CanShowSnackbar, Root {

    private val viewModel by viewModel<TargetsViewModel>()

    private val itemTouchHelper by lazy {
        ItemTouchHelper(ItemTouchHelperCallback())
    }

    private val adapter by lazy {
        TargetsAdapter(
            binding.targetsRecyclerview,
            emptyList(),
            itemTouchHelper::startDrag,
            viewModel::onItemClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupMonet()
        setupResult()
        setupFab()
        setupEmpty()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadClearingCache()
    }

    override fun onDestroyView() {
        binding.targetsRecyclerview.adapter = null
        super.onDestroyView()
    }

    private fun setupMonet() {
        binding.loading.loadingProgress.applyMonet()
    }

    private fun setupRecyclerView() = with(binding.targetsRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@TargetsFragment.adapter
        val fabMargin = resources.getDimension(R.dimen.fab_margin)
        applyBottomNavigationInset(fabMargin)
        itemTouchHelper.attachToRecyclerView(this)
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
                binding.targetsRecyclerview.isVisible = false
                binding.targetsEmpty.isVisible = false
                binding.targetsFabAdd.isVisible = false
            }
            is State.Loaded<*> -> {
                binding.loading.root.isVisible = false
                binding.targetsRecyclerview.isVisible = state.items.isNotEmpty()
                binding.targetsEmpty.isVisible = state.isEmpty
                binding.targetsFabAdd.isVisible = true
                adapter.items = state.items as List<ItemHolder<TargetHolder>>
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupFab() = with(binding.targetsFabAdd){
        binding.targetsFabAddContainer.applyBottomNavigationMarginShort()
        backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        whenResumed {
            onClicked().collect {
                viewModel.onAddClicked()
            }
        }
    }

    private fun setupEmpty() = with(binding.targetsEmptyLabel) {
        applyBottomNavigationMarginShort()
    }

    private fun setupResult() {
        setFragmentResultListener(REQUEST_KEY_TARGETS_ADD) { _, bundle ->
            val packageName = bundle.getString(RESULT_KEY_PACKAGE_NAME)
                ?: return@setFragmentResultListener
            val authority = bundle.getString(RESULT_KEY_AUTHORITY)
                ?: return@setFragmentResultListener
            val id = bundle.getString(RESULT_KEY_ID)
                ?: return@setFragmentResultListener
            val notificationAuthority = bundle.getString(RESULT_KEY_NOTIFICATION_AUTHORITY)
            val broadcastAuthority = bundle.getString(RESULT_KEY_BROADCAST_AUTHORITY)
            viewModel.addItem(authority, id, packageName, notificationAuthority, broadcastAuthority)
        }
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.menu_wallpaper_colour_picker).isVisible =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.menu_setup -> viewModel.rerunSetup()
            R.id.menu_wallpaper_colour_picker -> viewModel.onWallpaperColourPickerClicked()
        }
        return true
    }

    override fun setSnackbarVisible(visible: Boolean) {
        if(view == null) return
        whenResumed {
            binding.targetsFabAddSpace.isVisible = visible
        }
    }

    inner class ItemTouchHelperCallback: BaseManagerItemTouchHelperCallback<TargetHolder, ViewHolder>() {

        override val adapter by lazy {
            this@TargetsFragment.adapter
        }

        override val viewModel by lazy {
            this@TargetsFragment.viewModel
        }

    }

}
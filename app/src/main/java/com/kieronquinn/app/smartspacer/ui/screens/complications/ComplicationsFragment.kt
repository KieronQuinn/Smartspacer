package com.kieronquinn.app.smartspacer.ui.screens.complications

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
import com.kieronquinn.app.smartspacer.databinding.FragmentComplicationsBinding
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.CanShowSnackbar
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesOverflow
import com.kieronquinn.app.smartspacer.ui.base.Root
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter.ItemHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerItemTouchHelperCallback
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsViewModel.ComplicationHolder
import com.kieronquinn.app.smartspacer.ui.screens.complications.add.ComplicationsAddFragment
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMarginShort
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class ComplicationsFragment: BoundFragment<FragmentComplicationsBinding>(FragmentComplicationsBinding::inflate), ProvidesOverflow, CanShowSnackbar, LockCollapsed, Root {

    private val viewModel by viewModel<ComplicationsViewModel>()

    private val adapter by lazy {
        ComplicationsAdapter(
            binding.complicationsRecyclerview,
            emptyList(),
            itemTouchHelper::startDrag,
            viewModel::onItemClicked
        )
    }

    private val itemTouchHelper by lazy {
        ItemTouchHelper(ItemTouchHelperCallback())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupMonet()
        setupFab()
        setupEmpty()
        setupResult()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadClearingCache()
    }

    override fun onDestroyView() {
        binding.complicationsRecyclerview.adapter = null
        super.onDestroyView()
    }

    private fun setupMonet() {
        binding.loading.loadingProgress.applyMonet()
    }

    private fun setupRecyclerView() = with(binding.complicationsRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@ComplicationsFragment.adapter
        val fabMargin = resources.getDimension(com.kieronquinn.app.smartspacer.R.dimen.fab_margin)
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

    private fun handleState(state: BaseManagerViewModel.State) {
        when(state){
            is BaseManagerViewModel.State.Loading -> {
                binding.loading.root.isVisible = true
                binding.complicationsRecyclerview.isVisible = false
                binding.complicationsEmpty.isVisible = false
                binding.complicationsFabAdd.isVisible = false
            }
            is BaseManagerViewModel.State.Loaded<*> -> {
                binding.loading.root.isVisible = false
                binding.complicationsRecyclerview.isVisible = state.items.isNotEmpty()
                binding.complicationsEmpty.isVisible = state.isEmpty
                binding.complicationsFabAdd.isVisible = true
                adapter.items = state.items as List<ItemHolder<ComplicationHolder>>
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupFab() = with(binding.complicationsFabAdd){
        binding.complicationsFabAddContainer.applyBottomNavigationMarginShort()
        backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        whenResumed {
            onClicked().collect {
                viewModel.onAddClicked()
            }
        }
    }

    private fun setupEmpty() = with(binding.complicationsEmptyLabel) {
        applyBottomNavigationMarginShort()
    }

    private fun setupResult() {
        setFragmentResultListener(ComplicationsAddFragment.REQUEST_KEY_COMPLICATIONS_ADD) { _, bundle ->
            val packageName = bundle.getString(ComplicationsAddFragment.RESULT_KEY_PACKAGE_NAME)
                ?: return@setFragmentResultListener
            val authority = bundle.getString(ComplicationsAddFragment.RESULT_KEY_AUTHORITY)
                ?: return@setFragmentResultListener
            val id = bundle.getString(ComplicationsAddFragment.RESULT_KEY_ID)
                ?: return@setFragmentResultListener
            val notificationAuthority = bundle.getString(
                ComplicationsAddFragment.RESULT_KEY_NOTIFICATION_AUTHORITY
            )
            val broadcastAuthority = bundle.getString(
                ComplicationsAddFragment.RESULT_KEY_BROADCAST_AUTHORITY
            )
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
            binding.complicationsFabAddSpace.isVisible = visible
        }
    }

    inner class ItemTouchHelperCallback: BaseManagerItemTouchHelperCallback<ComplicationHolder, ComplicationsAdapter.ViewHolder>() {

        override val adapter by lazy {
            this@ComplicationsFragment.adapter
        }

        override val viewModel by lazy {
            this@ComplicationsFragment.viewModel
        }

    }

}
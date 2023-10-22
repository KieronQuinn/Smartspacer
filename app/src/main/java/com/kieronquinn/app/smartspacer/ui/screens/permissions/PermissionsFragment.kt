package com.kieronquinn.app.smartspacer.ui.screens.permissions

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentPermissionsBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.AllowAskEveryTimeOptions
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.AllowDenyOptions
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class PermissionsFragment: BoundFragment<FragmentPermissionsBinding>(FragmentPermissionsBinding::inflate), BackAvailable {

    private val viewModel by viewModel<PermissionsViewModel>()

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupLoading()
        setupState()
    }

    private fun setupRecyclerView() = with(binding.permissionsRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@PermissionsFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupLoading() = with(binding.permissionsLoadingProgress) {
        applyMonet()
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
                binding.permissionsLoading.isVisible = true
                binding.permissionsEmpty.isVisible = false
                binding.permissionsRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.permissionsLoading.isVisible = false
                binding.permissionsRecyclerView.isVisible = state.items.isNotEmpty()
                binding.permissionsEmpty.isVisible = state.items.isEmpty()
                adapter.update(state.loadItems(), binding.permissionsRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return items.map {
            when(it){
                is PermissionItem.Header ->  GenericSettingsItem.Header(getString(it.title))
                is PermissionItem.Card -> {
                    GenericSettingsItem.Card(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning),
                        getString(it.content)
                    )
                }
                is PermissionItem.WidgetPermission -> it.toSettingsItem()
                is PermissionItem.NotificationsPermission -> it.toSettingsItem()
                is PermissionItem.SmartspacePermission -> it.toSettingsItem()
            }
        }
    }

    private fun PermissionItem.WidgetPermission.toSettingsItem(): GenericSettingsItem {
        val onSet = { option: AllowAskEveryTimeOptions ->
            viewModel.onWidgetPermissionSet(packageName, option)
        }
        return GenericSettingsItem.Dropdown(
            title,
            getString(R.string.permissions_allow),
            null,
            AllowAskEveryTimeOptions.ALLOW,
            onSet,
            AllowAskEveryTimeOptions.values().toList(),
        ){ it.title }
    }

    private fun PermissionItem.NotificationsPermission.toSettingsItem(): GenericSettingsItem {
        val onSet = { option: AllowAskEveryTimeOptions ->
            viewModel.onNotificationsPermissionSet(packageName, option)
        }
        return GenericSettingsItem.Dropdown(
            title,
            getString(R.string.permissions_allow),
            null,
            AllowAskEveryTimeOptions.ALLOW,
            onSet,
            AllowAskEveryTimeOptions.values().toList(),
        ){ it.title }
    }

    private fun PermissionItem.SmartspacePermission.toSettingsItem(): GenericSettingsItem {
        val onSet = { option: AllowDenyOptions ->
            viewModel.onSmartspacePermissionSet(packageName, option)
        }
        return GenericSettingsItem.Dropdown(
            title,
            getString(R.string.permissions_allow),
            null,
            AllowDenyOptions.ALLOW,
            onSet,
            AllowDenyOptions.values().toList(),
        ){ it.title }
    }

    inner class Adapter: BaseSettingsAdapter(binding.permissionsRecyclerView, emptyList())

}
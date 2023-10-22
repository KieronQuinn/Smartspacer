package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.picker

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationRequirementWifiPickerBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Card
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Header
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.picker.WiFiRequirementConfigurationPickerViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class WiFiRequirementConfigurationPickerFragment: BoundFragment<FragmentConfigurationRequirementWifiPickerBinding>(FragmentConfigurationRequirementWifiPickerBinding::inflate), BackAvailable {

    private val viewModel by viewModel<WiFiRequirementConfigurationPickerViewModel>()
    private val args by navArgs<WiFiRequirementConfigurationPickerFragmentArgs>()

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupRecyclerView()
        setupLoading()
        setupSearch()
        setupSearchClear()
        viewModel.setupWithId(args.smartspacerId)
        viewModel.refresh()
    }

    private fun setupRecyclerView() = with(binding.configurationRequirementWifiPickerRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@WiFiRequirementConfigurationPickerFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupLoading() = with(binding.configurationRequirementWifiPickerLoading.loadingProgress) {
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

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> {
                configurationRequirementWifiPickerLoading.root.isVisible = true
                configurationRequirementWifiPickerRecyclerView.isVisible = false
                configurationRequirementWifiPickerSearch.root.isVisible = false
            }
            is State.Loaded -> {
                configurationRequirementWifiPickerLoading.root.isVisible = false
                configurationRequirementWifiPickerRecyclerView.isVisible = true
                configurationRequirementWifiPickerSearch.root.isVisible = true
                adapter.update(state.loadItems(), configurationRequirementWifiPickerRecyclerView)
            }
        }
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.configurationRequirementWifiPickerSearch.searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.configurationRequirementWifiPickerSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.configurationRequirementWifiPickerSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.configurationRequirementWifiPickerSearch.searchBox.run {
            this.text?.let {
                it.clear()
                it.append(text)
            } ?: setText(text)
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        val availableNetworkItems = listOfNotNull(
            if(!isSearching || availableNetworks.isNotEmpty() || connectedNetwork != null){
                Header(getString(R.string.requirement_wifi_configuration_picker_available))
            }else null,
            connectedNetwork?.let {
                Setting(
                    it.ssid.ifNullOrEmpty {
                        getString(R.string.requirement_wifi_configuration_picker_unknown_network)
                    },
                    it.mac ?: "",
                    null
                ) {
                    viewModel.onNetworkClicked(it)
                }
            },
            *availableNetworks.map {
                Setting(
                    it.ssid.ifNullOrEmpty {
                        getString(R.string.requirement_wifi_configuration_picker_unknown_network)
                    },
                    it.mac ?: "",
                    null
                ) {
                    viewModel.onNetworkClicked(it)
                }
            }.toTypedArray(),
            if(availableNetworks.isEmpty() && !isSearching){
                Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                    getString(R.string.requirement_wifi_configuration_picker_available_empty)
                )
            }else null
        )
        val savedNetworksItems = listOfNotNull(
            if(savedNetworks.isNotEmpty()) {
                Header(getString(R.string.requirement_wifi_configuration_picker_saved))
            }else null,
            *savedNetworks.map {
                Setting(
                    it.ssid.ifNullOrEmpty {
                        getString(R.string.requirement_wifi_configuration_picker_unknown_network)
                    },
                    it.mac ?: "",
                    null
                ) {
                    viewModel.onNetworkClicked(it)
                }
            }.toTypedArray()
        )
        return availableNetworkItems + savedNetworksItems
    }

    private fun String?.ifNullOrEmpty(alternative: () -> String): String {
        return when {
            this == null -> alternative()
            this.isEmpty() -> alternative()
            else -> this
        }
    }

    inner class Adapter:
        BaseSettingsAdapter(binding.configurationRequirementWifiPickerRecyclerView, emptyList())

}
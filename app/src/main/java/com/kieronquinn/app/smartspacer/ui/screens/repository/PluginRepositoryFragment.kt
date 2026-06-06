package com.kieronquinn.app.smartspacer.ui.screens.repository

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate.BlurMode
import com.kieronquinn.app.smartspacer.databinding.FragmentPluginRepositoryBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.CanShowSnackbar
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesOverflow
import com.kieronquinn.app.smartspacer.ui.base.Root
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.repository.PluginRepositoryViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.applyMonet
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onSelected
import com.kieronquinn.app.smartspacer.utils.extensions.selectTab
import com.kieronquinn.app.smartspacer.utils.extensions.setRoundedOutline
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.utils.extensions.withAlpha
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class PluginRepositoryFragment: BoundFragment<FragmentPluginRepositoryBinding>(FragmentPluginRepositoryBinding::inflate), Root, LockCollapsed, CanShowSnackbar, ProvidesOverflow {

    private val viewModel by viewModel<PluginRepositoryViewModel>()

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        setupMonet()
        setupTabs()
        setupEmpty()
        setupRecyclerView()
        setupState()
        setupSearch()
        setupSearchClear()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload(false)
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        if (!BuildConfig.DEBUG) return
        menuInflater.inflate(R.menu.menu_plugins, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_plugins_clear_seen -> viewModel.clearSeenPlugins()
        }
        return true
    }

    private fun setupMonet() {
        binding.pluginRepositoryTabs.applyMonet()
        binding.pluginRepositoryLoadingProgress.applyMonet()
        binding.pluginRepositoryEmptyButton.applyMonet()
        binding.pluginRepositorySearch.searchBox.applyMonet()
    }

    private fun setupTabs() = with(binding.pluginRepositoryTabs) {
        val index = if(viewModel.state.value.isAvailableTab()) 1 else 0
        selectTab(index)
        whenResumed {
            onSelected().collect {
                viewModel.setSelectedTab(it)
            }
        }
    }

    private fun setupRecyclerView() = with(binding.pluginRepositoryRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@PluginRepositoryFragment.adapter
        val margin = resources.getDimension(R.dimen.margin_16)
        val corners = resources.getDimension(R.dimen.margin_24)
        applyBottomNavigationInset(margin)
        setRoundedOutline(corners, margin.toInt())
        clipToOutline = true
    }

    private fun setupEmpty() = with(binding.pluginRepositoryEmpty) {
        applyBottomNavigationInset()
    }

    private fun setupSearch() = with(binding.pluginRepositorySearch) {
        setSearchText(viewModel.getSearchTerm())
        val blurBackground = requireView().background
        val searchBackground = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        searchBox.backgroundTintList = ColorStateList.valueOf(searchBackground)
        searchBoxBlur.setRoundedOutline(resources.getDimension(R.dimen.margin_24))
        searchBoxBlur.clipToOutline = true
        val blur = BlurDelegate.get(
            BlurMode.View(searchBoxBlur, binding.pluginRepositoryBlurTarget, blurBackground),
            lifecycleScope
        )
        blur.setBlur(1f)
        whenResumed {
            blur.blurAvailable.collect { available ->
                searchBox.backgroundTintList = if (available) {
                    ColorStateList.valueOf(searchBackground.withAlpha(0.75f))
                } else {
                    ColorStateList.valueOf(searchBackground)
                }
            }
        }
        whenResumed {
            searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.pluginRepositorySearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.pluginRepositorySearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.pluginRepositorySearch.searchBox.run {
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
                binding.pluginRepositoryLoading.isVisible = true
                binding.pluginRepositoryLoaded.isVisible = false
                binding.pluginRepositoryEmpty.isVisible = false
            }
            is State.Loaded -> {
                binding.pluginRepositoryLoading.isVisible = false
                binding.pluginRepositoryLoaded.isVisible = true
                binding.pluginRepositoryEmpty.isVisible = state.isEmpty
                if(state.isEmpty){
                    val title = when {
                        state.shouldOfferBrowse -> {
                            R.string.plugin_repository_empty_title_browse
                        }
                        state.isSearchEmpty -> {
                            R.string.plugin_repository_empty_title_empty
                        }
                        else -> {
                            R.string.plugin_repository_empty_title_not_found
                        }
                    }
                    val subtitle = when {
                        state.shouldOfferBrowse -> {
                            R.string.plugin_repository_empty_subtitle_browse
                        }
                        state.isSearchEmpty -> {
                            R.string.plugin_repository_empty_subtitle_empty
                        }
                        else -> {
                            R.string.plugin_repository_empty_subtitle_not_found
                        }
                    }
                    val button = when {
                        state.isSearchEmpty -> {
                            R.string.plugin_repository_empty_button_empty
                        }
                        else -> {
                            R.string.plugin_repository_empty_button_browse
                        }
                    }
                    binding.pluginRepositoryEmptyTitle.setText(title)
                    binding.pluginRepositoryEmptySubtitle.setText(subtitle)
                    binding.pluginRepositoryEmptyButton.setText(button)
                    binding.pluginRepositoryEmptyButton.isVisible =
                        state.shouldOfferBrowse || state.isSearchEmpty
                    whenResumed {
                        binding.pluginRepositoryEmptyButton.onClicked().collect {
                            if(state.isSearchEmpty){
                                viewModel.reload(true)
                            }else{
                                binding.pluginRepositoryTabs.selectTab(1)
                            }
                        }
                    }
                }
                adapter.update(state.loadItems(), binding.pluginRepositoryRecyclerview)
            }
        }
    }

    private fun State.isAvailableTab(): Boolean {
        return (this as? State.Loaded)?.isAvailableTab ?: false
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return items.map {
            it.toSettingsItem(isAvailableTab)
        }
    }

    private fun Plugin.toSettingsItem(isAvailable: Boolean): BaseSettingsItem {
        val content = SpannableStringBuilder().apply {
            if(this@toSettingsItem is Plugin.Remote){
                if(updateAvailable) {
                    append(
                        getString(R.string.plugin_repository_update_available),
                        StyleSpan(Typeface.BOLD),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" • ")
                } else if (new) {
                    append(
                        getString(R.string.plugin_repository_new),
                        StyleSpan(Typeface.BOLD),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" • ")
                }
                append(author, StyleSpan(Typeface.ITALIC), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                appendLine()
                if(recommendedApps.isNotEmpty() && isAvailable){
                    val recommended = recommendedApps.joinToString(", ")
                    append(
                        getString(R.string.plugin_repository_recommended),
                        StyleSpan(Typeface.BOLD),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" ")
                    append(recommended)
                    appendLine()
                }
                append(description)
            }else{
                append(packageName)
            }
        }
        return GenericSettingsItem.Setting(name, content, null){
            viewModel.onPluginClicked(this)
        }
    }

    inner class Adapter: BaseSettingsAdapter(binding.pluginRepositoryRecyclerview, emptyList())

}
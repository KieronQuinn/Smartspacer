package com.kieronquinn.app.smartspacer.ui.screens.repository

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentPluginRepositoryBinding
import com.kieronquinn.app.smartspacer.databinding.FragmentPluginRepositoryPageBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.CanShowSnackbar
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.Root
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.repository.PluginRepositoryViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class PluginRepositoryFragment :
    BoundFragment<FragmentPluginRepositoryBinding>(FragmentPluginRepositoryBinding::inflate),
    Root, LockCollapsed, CanShowSnackbar {

    private val viewModel by viewModel<PluginRepositoryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupPager()
        setupSearch()
        setupSearchClear()
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload(false)
    }

    private fun setupMonet() {
        val secondaryBackground = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.pluginRepositoryAppbar.setBackgroundColor(secondaryBackground)
        binding.pluginRepositorySearch.searchBox.backgroundTintList =
            android.content.res.ColorStateList.valueOf(secondaryBackground)
    }

    private fun setupPager() {
        binding.pluginRepositoryPager.adapter = PluginPagerAdapter(this)
        binding.pluginRepositoryPager.offscreenPageLimit = 1
        TabLayoutMediator(
            binding.pluginRepositoryTabs,
            binding.pluginRepositoryPager
        ) { tab, position ->
            tab.text = getString(
                if (position == 0) R.string.plugin_repository_tab_installed
                else R.string.plugin_repository_tab_available
            )
        }.attach()
    }

    /** Switches the ViewPager2 to the given page position. Used by page Fragments. */
    fun switchToPage(position: Int) {
        binding.pluginRepositoryPager.currentItem = position
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.pluginRepositorySearch.searchBox.onChanged().collect {
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
            viewModel.state.collect { handleState(it) }
        }
    }

    private fun handleState(state: State) {
        binding.pluginRepositoryLoading.isVisible = state is State.Loading
    }

    // ── ViewPager2 adapter ─────────────────────────────────────────────────

    private inner class PluginPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment =
            PluginListPage.newInstance(isAvailable = position == 1)
    }
}

// ── Per-tab page Fragment ──────────────────────────────────────────────────

/**
 * A single tab page inside the Plugin Repository ViewPager2.
 * Shares the parent Fragment's ViewModel via Koin's ownerProducer.
 */
class PluginListPage : Fragment() {

    companion object {
        private const val ARG_IS_AVAILABLE = "is_available"

        fun newInstance(isAvailable: Boolean) = PluginListPage().also {
            it.arguments = Bundle().apply { putBoolean(ARG_IS_AVAILABLE, isAvailable) }
        }
    }

    private val isAvailable get() = requireArguments().getBoolean(ARG_IS_AVAILABLE)

    private val viewModel: PluginRepositoryViewModel by viewModel(
        ownerProducer = { requireParentFragment() }
    )

    private val adapter by lazy {
        PageAdapter()
    }

    private var _binding: FragmentPluginRepositoryPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPluginRepositoryPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() = with(binding.pluginPageRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@PluginListPage.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect { handleState(it) }
        }
    }

    private fun handleState(state: State) {
        when (state) {
            is State.Loading -> {
                binding.pluginPageEmpty.isVisible = false
            }
            is State.Loaded -> {
                val items = if (isAvailable) state.availableItems else state.installedItems
                val empty = items.isEmpty()
                binding.pluginPageEmpty.isVisible = empty
                if (empty) {
                    val titleRes = when {
                        !isAvailable && state.isSearchEmpty ->
                            R.string.plugin_repository_empty_title_browse
                        state.isSearchEmpty ->
                            R.string.plugin_repository_empty_title_empty
                        else ->
                            R.string.plugin_repository_empty_title_not_found
                    }
                    val subtitleRes = when {
                        !isAvailable && state.isSearchEmpty ->
                            R.string.plugin_repository_empty_subtitle_browse
                        state.isSearchEmpty ->
                            R.string.plugin_repository_empty_subtitle_empty
                        else ->
                            R.string.plugin_repository_empty_subtitle_not_found
                    }
                    binding.pluginPageEmptyTitle.setText(titleRes)
                    binding.pluginPageEmptySubtitle.setText(subtitleRes)
                    val showBrowse = !isAvailable && state.isSearchEmpty
                    binding.pluginPageEmptyButton.isVisible = showBrowse
                    if (showBrowse) {
                        binding.pluginPageEmptyButton.setText(R.string.plugin_repository_empty_button_browse)
                        binding.pluginPageEmptyButton.setOnClickListener {
                            (requireParentFragment() as PluginRepositoryFragment).switchToPage(1)
                        }
                    }
                }
                adapter.update(
                    items.map { it.toSettingsItem() },
                    binding.pluginPageRecyclerview
                )
            }
        }
    }

    private fun Plugin.toSettingsItem(): BaseSettingsItem {
        val content = SpannableStringBuilder().apply {
            if (this@toSettingsItem is Plugin.Remote) {
                if (updateAvailable) {
                    append(
                        getString(R.string.plugin_repository_update_available),
                        StyleSpan(Typeface.BOLD),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" • ")
                }
                append(author, StyleSpan(Typeface.ITALIC), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                appendLine()
                if (recommendedApps.isNotEmpty() && isAvailable) {
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
            } else {
                append(packageName)
            }
        }
        return GenericSettingsItem.Setting(name, content, null) {
            viewModel.onPluginClicked(this)
        }
    }

    private inner class PageAdapter : BaseSettingsAdapter(binding.pluginPageRecyclerview, emptyList())
}

package com.kieronquinn.app.smartspacer.ui.screens.expanded.tabs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentExpandedTabSettingsBinding
import com.kieronquinn.app.smartspacer.databinding.ItemTabSettingsRowBinding
import com.kieronquinn.app.smartspacer.model.expanded.ExpandedTabConfig
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedTabRepository
import org.koin.android.ext.android.inject
import java.util.UUID

/**
 * Settings screen for configuring widget tab configurations.
 *
 * Allows users to:
 *  - Add new tabs (each mapped to one AppWidget instance by appWidgetId)
 *  - Edit each tab's label
 *  - Assign a specific AppWidget instance (picked from those already bound
 *    in the expanded view via ExpandedRepository.expandedCustomAppWidgets)
 *  - Delete tabs
 *  - Save the updated tab list (auto-saved when leaving via the back button)
 *
 * Uses Material You Expressive design: large headline, tonal card rows,
 * filled FAB, tonal delete buttons.
 */
class ExpandedTabSettingsFragment : Fragment() {

    private var _binding: FragmentExpandedTabSettingsBinding? = null
    private val binding get() = _binding!!

    private val tabRepository by inject<ExpandedTabRepository>()
    private val expandedRepository by inject<ExpandedRepository>()

    /** Working copy of the tab list — mutated by the adapter, saved on back. */
    private val currentTabs = mutableListOf<ExpandedTabConfig>()

    private lateinit var adapter: TabSettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpandedTabSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load current tabs into the working list
        currentTabs.clear()
        currentTabs.addAll(tabRepository.getTabs())

        setupToolbar()
        setupRecyclerView()
        setupAddFab()
    }

    private fun setupToolbar() {
        binding.tabSettingsToolbar.setNavigationOnClickListener {
            saveAndPop()
        }
    }

    private fun setupRecyclerView() {
        adapter = TabSettingsAdapter(currentTabs, ::onPickWidget, ::onDeleteTab)
        binding.tabSettingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.tabSettingsRecycler.adapter = adapter
    }

    private fun setupAddFab() {
        binding.tabSettingsAddFab.setOnClickListener {
            val newTab = ExpandedTabConfig(
                id = UUID.randomUUID().toString(),
                label = "Tab ${currentTabs.size + 1}",
                appWidgetId = -1
            )
            currentTabs.add(newTab)
            adapter.notifyItemInserted(currentTabs.lastIndex)
            binding.tabSettingsRecycler.scrollToPosition(currentTabs.lastIndex)
        }
    }

    private fun onPickWidget(position: Int) {
        // Collect already-bound custom widgets from ExpandedRepository, show a simple
        // AlertDialog picker so the user can assign one to this tab.
        collectBoundWidgets { widgets ->
            if (widgets.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.expanded_tab_settings_no_widgets),
                    Toast.LENGTH_LONG
                ).show()
                return@collectBoundWidgets
            }
            val labels = widgets.map { widget ->
                getString(R.string.expanded_tab_settings_widget_assigned, widget.appWidgetId)
            }.toTypedArray()

            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.expanded_tab_settings_pick_widget)
                .setItems(labels) { _, which ->
                    val chosen = widgets[which]
                    val updated = currentTabs[position].copy(appWidgetId = chosen.appWidgetId)
                    currentTabs[position] = updated
                    adapter.notifyItemChanged(position)
                }
                .show()
        }
    }

    private fun onDeleteTab(position: Int) {
        if (position < 0 || position >= currentTabs.size) return
        currentTabs.removeAt(position)
        adapter.notifyItemRemoved(position)
        // Re-number remaining tabs' labels if they still have the default pattern
        adapter.notifyItemRangeChanged(position, currentTabs.size - position)
    }

    private fun collectBoundWidgets(block: (List<com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget>) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val widgets = expandedRepository.expandedCustomAppWidgets.first()
            block(widgets)
        }
    }

    private fun saveAndPop() {
        // Strip any tabs that have no widget assigned yet
        val validTabs = currentTabs.filter { it.appWidgetId != -1 }
        tabRepository.saveTabs(validTabs)
        Toast.makeText(
            requireContext(),
            getString(R.string.expanded_tab_settings_saved),
            Toast.LENGTH_SHORT
        ).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Adapter
    // ──────────────────────────────────────────────────────────────────────

    private inner class TabSettingsAdapter(
        private val tabs: MutableList<ExpandedTabConfig>,
        private val onPickWidget: (Int) -> Unit,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<TabSettingsAdapter.VH>() {

        inner class VH(val binding: ItemTabSettingsRowBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            ItemTabSettingsRowBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        override fun getItemCount() = tabs.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val tab = tabs[position]
            with(holder.binding) {
                // Tab number chip
                tabRowIndexChip.text = "Tab ${position + 1}"

                // Label field — avoid recursive TextWatcher triggering
                tabRowLabelEdit.removeTextChangedListener(holder.itemView.tag as? TextWatcher)
                tabRowLabelEdit.setText(tab.label)
                val watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val idx = holder.bindingAdapterPosition
                        if (idx != RecyclerView.NO_ID.toInt()) {
                            tabs[idx] = tabs[idx].copy(label = s?.toString() ?: "")
                        }
                    }
                }
                holder.itemView.tag = watcher
                tabRowLabelEdit.addTextChangedListener(watcher)

                // Widget assignment label
                if (tab.appWidgetId != -1) {
                    tabRowWidgetLabel.isVisible = true
                    tabRowWidgetLabel.text =
                        getString(R.string.expanded_tab_settings_widget_assigned, tab.appWidgetId)
                } else {
                    tabRowWidgetLabel.isVisible = false
                }

                tabRowPickWidget.setOnClickListener { onPickWidget(holder.bindingAdapterPosition) }
                tabRowDelete.setOnClickListener { onDelete(holder.bindingAdapterPosition) }
            }
        }
    }

}

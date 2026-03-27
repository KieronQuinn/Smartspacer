package com.kieronquinn.app.smartspacer.ui.screens.expanded.tabs

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kieronquinn.app.smartspacer.databinding.FragmentExpandedTabIconPickerBinding
import com.kieronquinn.app.smartspacer.databinding.ItemIconPickerBinding
import com.kieronquinn.app.smartspacer.utils.extensions.MaterialSymbolsHelper
import com.kieronquinn.app.smartspacer.utils.extensions.MaterialSymbolsHelper.Symbol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-height bottom sheet for picking a Material Symbols Outlined icon.
 *
 * Launched from [ExpandedTabSettingsFragment] and returns the selected codepoint
 * via the Fragment Result API (key: [RESULT_KEY]).
 */
class ExpandedTabIconPickerBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        const val RESULT_KEY = "icon_picker_result"
        const val KEY_POSITION = "position"
        const val KEY_CODEPOINT = "codepoint"

        private const val ARG_POSITION = "position"
        private const val ARG_CURRENT_CODEPOINT = "current_codepoint"
        private const val SEARCH_DEBOUNCE_MS = 200L

        fun newInstance(position: Int, currentCodepoint: Int?) =
            ExpandedTabIconPickerBottomSheetFragment().apply {
                arguments = bundleOf(
                    ARG_POSITION to position,
                    ARG_CURRENT_CODEPOINT to (currentCodepoint ?: -1)
                )
            }
    }

    private var _binding: FragmentExpandedTabIconPickerBinding? = null
    private val binding get() = _binding!!

    private val position: Int by lazy { requireArguments().getInt(ARG_POSITION) }
    private val currentCodepoint: Int? by lazy {
        requireArguments().getInt(ARG_CURRENT_CODEPOINT, -1).takeIf { it != -1 }
    }

    private var symbols: List<Symbol> = emptyList()
    private var typeface: Typeface? = null
    private lateinit var adapter: IconPickerAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpandedTabIconPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expandToFullHeight()
        setupClearButton()
        setupSearch()
        loadIconsAsync()
    }

    private fun expandToFullHeight() {
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun setupClearButton() {
        binding.iconPickerClear.isVisible = currentCodepoint != null
        binding.iconPickerClear.setOnClickListener {
            deliverResult(-1)
        }
    }

    private fun setupSearch() {
        binding.iconPickerSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    filterIcons(s?.toString() ?: "")
                }
            }
        })
    }

    private fun loadIconsAsync() {
        binding.iconPickerLoading.isVisible = true
        binding.iconPickerRecycler.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext().applicationContext
            val (tf, list) = withContext(Dispatchers.IO) {
                Pair(
                    MaterialSymbolsHelper.getTypeface(ctx),
                    MaterialSymbolsHelper.getSymbols(ctx)
                )
            }
            typeface = tf
            symbols = list

            adapter = IconPickerAdapter(list, tf, currentCodepoint, ::deliverResult)
            binding.iconPickerRecycler.apply {
                layoutManager = GridLayoutManager(requireContext(), 5)
                setHasFixedSize(true)
                setItemViewCacheSize(50)
                adapter = this@ExpandedTabIconPickerBottomSheetFragment.adapter
            }

            binding.iconPickerLoading.isVisible = false
            binding.iconPickerRecycler.isVisible = true
        }
    }

    private fun filterIcons(query: String) {
        if (!::adapter.isInitialized) return
        val filtered = if (query.isBlank()) symbols
        else symbols.filter { it.name.contains(query.lowercase().trim()) }

        adapter.update(filtered)
        binding.iconPickerEmpty.isVisible = filtered.isEmpty()
        binding.iconPickerRecycler.isVisible = filtered.isNotEmpty()
        if (filtered.isNotEmpty()) binding.iconPickerRecycler.scrollToPosition(0)
    }

    private fun deliverResult(codepoint: Int) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            bundleOf(KEY_POSITION to position, KEY_CODEPOINT to codepoint)
        )
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Adapter
    // ──────────────────────────────────────────────────────────────────────

    private inner class IconPickerAdapter(
        private var items: List<Symbol>,
        private val typeface: Typeface,
        private val selectedCodepoint: Int?,
        private val onPick: (Int) -> Unit
    ) : RecyclerView.Adapter<IconPickerAdapter.VH>() {

        inner class VH(val binding: ItemIconPickerBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            ItemIconPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val symbol = items[position]
            with(holder.binding) {
                iconPickerSymbol.typeface = typeface
                iconPickerSymbol.text = symbol.char
                iconPickerName.text = symbol.displayName

                val isSelected = symbol.codepoint == selectedCodepoint
                root.alpha = if (isSelected) 1f else 0.87f
                root.setOnClickListener { onPick(symbol.codepoint) }
            }
        }

        fun update(newItems: List<Symbol>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}

package com.kieronquinn.app.smartspacer.ui.screens.expanded.tabs

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentExpandedTabSettingsBinding
import com.kieronquinn.app.smartspacer.databinding.ItemTabSettingsRowBinding
import com.kieronquinn.app.smartspacer.model.expanded.ExpandedTabConfig
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedTabRepository
import com.kieronquinn.app.smartspacer.ui.activities.ExpandedActivity
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.utils.extensions.MaterialSymbolsHelper
import com.kieronquinn.app.smartspacer.utils.extensions.allowBackground
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.UUID

/**
 * Settings screen for configuring widget tab configurations.
 */
class ExpandedTabSettingsFragment : Fragment(), ProvidesBack {

    private var _binding: FragmentExpandedTabSettingsBinding? = null
    private val binding get() = _binding!!

    private val tabRepository by inject<ExpandedTabRepository>()
    private val expandedRepository by inject<ExpandedRepository>()

    private val currentTabs = mutableListOf<ExpandedTabConfig>()
    private lateinit var adapter: TabSettingsAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var pendingPickPosition = -1
    private var pendingReadYouWidgetId = -1

    private val widgetBindResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            launchReadYouConfig()
        } else {
            expandedRepository.deallocateAppWidgetId(pendingReadYouWidgetId)
            pendingReadYouWidgetId = -1
            pendingPickPosition = -1
        }
    }

    private val widgetConfigResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pos = pendingPickPosition
        val widgetId = pendingReadYouWidgetId
        if (result.resultCode == Activity.RESULT_OK && pos >= 0 && widgetId != -1) {
            currentTabs[pos] = currentTabs[pos].copy(appWidgetId = widgetId)
            adapter.notifyItemChanged(pos)
        } else {
            expandedRepository.deallocateAppWidgetId(widgetId)
        }
        pendingReadYouWidgetId = -1
        pendingPickPosition = -1
    }

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
        currentTabs.clear()
        currentTabs.addAll(tabRepository.getTabs())
        // Intercept system back at the dispatcher level so only ONE press is needed
        // to exit, rather than relying solely on ProvidesBack which requires two presses
        // in some navigation configurations.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { saveAndPop() }
            }
        )
        setupToolbar()
        setupHeaderBackground()
        setupRecyclerView()
        setupAddFab()
        setupInsets()
        setupIconPickerResult()
    }

    override fun onBackPressed(): Boolean {
        saveAndPop()
        return true
    }

    private fun setupToolbar() {
        binding.tabSettingsBackBtn.setOnClickListener { saveAndPop() }
        binding.tabSettingsSettingsBtn.setOnClickListener {
            startActivity(
                android.content.Intent(requireContext(),
                    com.kieronquinn.app.smartspacer.ui.activities.MainActivity::class.java
                ).apply {
                    action = android.content.Intent.ACTION_VIEW
                    data = android.net.Uri.parse("smartspacer://expanded")
                    putExtra(com.kieronquinn.app.smartspacer.ui.activities.MainActivity.EXTRA_SKIP_SPLASH, true)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private fun setupHeaderBackground() {
        val ctx = requireContext()
        val colorBg = ctx.getAttrColor(android.R.attr.colorBackground)

        // Status bar space stays fully opaque.
        binding.tabSettingsStatusBarSpace.setBackgroundColor(colorBg)

        // Toolbar fades 100% → 0% top-to-bottom so content below bleeds through.
        binding.tabSettingsToolbar.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(colorBg, Color.TRANSPARENT)
        )

        // Gradient strip below the toolbar is transparent — the toolbar gradient is enough.
        binding.tabSettingsGradient.background = null
    }

    private fun setupRecyclerView() {
        adapter = TabSettingsAdapter(currentTabs, ::onPickWidget, ::onPickIcon, ::onDeleteTab)
        binding.tabSettingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.tabSettingsRecycler.adapter = adapter
        binding.tabSettingsRecycler.addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    if (parent.getChildAdapterPosition(view) > 0) outRect.top = 16.dp
                }
            }
        )

        val dragCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                if (from < to) {
                    for (i in from until to) Collections.swap(currentTabs, i, i + 1)
                } else {
                    for (i in from downTo to + 1) Collections.swap(currentTabs, i, i - 1)
                }
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    binding.tabSettingsRecycler.itemAnimator = null
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                recyclerView.itemAnimator = DefaultItemAnimator()
                viewHolder.itemView.alpha = 1f
                val pos = viewHolder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) recyclerView.smoothScrollToPosition(pos)
            }

            // Cards here are much taller than on the targets/complications pages.
            // Lower the swap threshold so the user doesn't have to drag half a tall card
            // past its neighbour before the swap fires.
            override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder) = 0.15f

            // Cap auto-scroll speed — the default ramp is designed for small rows and
            // is far too fast for the tall cards on this page.
            override fun interpolateOutOfBoundsScroll(
                recyclerView: RecyclerView,
                viewSize: Int,
                viewSizeOutOfBounds: Int,
                totalSize: Int,
                msSinceStartScroll: Long
            ): Int {
                val direction = if (viewSizeOutOfBounds > 0) 1 else -1
                return direction * 6.dp
            }

            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(dragCallback)
        itemTouchHelper.attachToRecyclerView(binding.tabSettingsRecycler)
    }

    private fun setupAddFab() {
        binding.tabSettingsAddFab.setOnClickListener {
            val newTab = ExpandedTabConfig(
                id = UUID.randomUUID().toString(),
                label = "",
                appWidgetId = -1
            )
            currentTabs.add(newTab)
            adapter.notifyItemInserted(currentTabs.lastIndex)
            binding.tabSettingsRecycler.scrollToPosition(currentTabs.lastIndex)
        }
    }

    private fun setupInsets() {
        // Status bar space height + recycler top padding
        binding.tabSettingsStatusBarSpace.onApplyInsets { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.updateLayoutParams { height = statusBarHeight }
            // header = statusBar + toolbar (16+40+16=72dp) + 16dp gap = statusBar + 88dp
            binding.tabSettingsRecycler.updatePadding(top = statusBarHeight + 88.dp)
        }
        // FAB: 16dp above system nav bar
        binding.tabSettingsAddFab.onApplyInsets { fab, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight + 16.dp
            }
        }
        // Recycler bottom padding:
        //   Keyboard closed → clear the FAB (56dp) + 16dp gap + 16dp nav margin = sysBottom + 88dp
        //   Keyboard open   → 64dp clearance above the keyboard (FAB is irrelevant)
        // smoothScrollToPosition (not scrollToPosition) scrolls only enough to make the item
        // visible, anchoring the card's bottom to the visible area edge (64dp above the keyboard).
        // scrollToPosition snaps to the top of the list, which is why the card ended up too far up.
        binding.tabSettingsRecycler.onApplyInsets { view, insets ->
            val sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            view.updatePadding(bottom = if (imeVisible) imeBottom + 16.dp else sysBottom + 88.dp)
            if (imeVisible) {
                val rv = view as RecyclerView
                val focused = rv.findFocus()
                if (focused != null) {
                    val vh = rv.findContainingViewHolder(focused)
                    val pos = vh?.bindingAdapterPosition ?: RecyclerView.NO_POSITION
                    if (pos != RecyclerView.NO_POSITION) {
                        // Post so the padding change has been laid out before we measure.
                        // scrollToPositionWithOffset anchors the item's TOP at `offset` from
                        // the content start, placing its bottom 16dp above the keyboard
                        // regardless of whether it's the last item or not.
                        rv.post {
                            val lm = rv.layoutManager as? LinearLayoutManager ?: return@post
                            val itemHeight = rv.findViewHolderForAdapterPosition(pos)
                                ?.itemView?.height ?: run {
                                    rv.smoothScrollToPosition(pos)
                                    return@post
                                }
                            val visibleHeight = rv.height - rv.paddingTop - rv.paddingBottom
                            val offset = (visibleHeight - itemHeight - 16.dp).coerceAtLeast(0)
                            lm.scrollToPositionWithOffset(pos, offset)
                        }
                    }
                }
            }
        }
    }

    private fun setupIconPickerResult() {
        childFragmentManager.setFragmentResultListener(
            ExpandedTabIconPickerBottomSheetFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val pos = bundle.getInt(ExpandedTabIconPickerBottomSheetFragment.KEY_POSITION)
            val cp = bundle.getInt(ExpandedTabIconPickerBottomSheetFragment.KEY_CODEPOINT, -1)
            if (pos < 0 || pos >= currentTabs.size) return@setFragmentResultListener
            currentTabs[pos] = currentTabs[pos].copy(iconCodepoint = if (cp == -1) null else cp)
            adapter.notifyItemChanged(pos)
        }
    }

    private fun onPickWidget(position: Int) {
        pendingPickPosition = position
        val id = expandedRepository.allocateAppWidgetId()
        pendingReadYouWidgetId = id
        val provider = ComponentName(
            "me.ash.reader",
            "me.ash.reader.ui.widget.ArticleListWidgetReceiver"
        )
        if (expandedRepository.bindAppWidgetIdIfAllowed(id, provider)) {
            launchReadYouConfig()
        } else {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            widgetBindResult.launch(bindIntent)
        }
    }

    private fun launchReadYouConfig() {
        val id = pendingReadYouWidgetId
        if (id == -1) return
        // createConfigIntentSender delegates to an AIDL method that can return null when
        // the widget has no configure activity (common for Glance-based widgets like ReadYou).
        // The Kotlin non-null return type causes an NPE if we don't guard here.
        val intentSender = try {
            expandedRepository.createConfigIntentSender(id)
        } catch (_: Exception) {
            null
        }
        if (intentSender == null) {
            // No configure activity — accept the widget as-is.
            val pos = pendingPickPosition
            if (pos >= 0 && pos < currentTabs.size) {
                currentTabs[pos] = currentTabs[pos].copy(appWidgetId = id)
                adapter.notifyItemChanged(pos)
            }
            pendingReadYouWidgetId = -1
            pendingPickPosition = -1
            return
        }
        widgetConfigResult.launch(
            IntentSenderRequest.Builder(intentSender).build(),
            ActivityOptionsCompat.makeBasic().allowBackground()
        )
    }

    private fun onPickIcon(position: Int) {
        val current = currentTabs.getOrNull(position) ?: return
        ExpandedTabIconPickerBottomSheetFragment
            .newInstance(position, current.iconCodepoint)
            .show(childFragmentManager, "icon_picker")
    }

    private fun onDeleteTab(position: Int) {
        if (position < 0 || position >= currentTabs.size) return
        currentTabs.removeAt(position)
        adapter.notifyItemRemoved(position)
        adapter.notifyItemRangeChanged(position, currentTabs.size - position)
    }

    private fun saveAndPop() {
        val validTabs = currentTabs.filter { it.appWidgetId != -1 }
        tabRepository.saveTabs(validTabs)
        Toast.makeText(
            requireContext(),
            getString(R.string.expanded_tab_settings_saved),
            Toast.LENGTH_SHORT
        ).show()
        // Close the entire expanded view rather than just popping to ExpandedFragment,
        // which would require a second back press to dismiss the activity.
        val activity = requireActivity()
        val isOverlay = (activity as? ExpandedActivity)?.let { ExpandedActivity.isOverlay(it) } ?: false
        if (isOverlay) {
            viewLifecycleOwner.lifecycleScope.launch {
                expandedRepository.onOverlayBackPressed()
            }
            activity.finish()
        } else {
            activity.overridePendingTransition(0, 0)
            activity.finishAndRemoveTask()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ────────────────────────────────────────────────────────────────────────
    // Adapter
    // ────────────────────────────────────────────────────────────────────────

    private inner class TabSettingsAdapter(
        private val tabs: MutableList<ExpandedTabConfig>,
        private val onPickWidget: (Int) -> Unit,
        private val onPickIcon: (Int) -> Unit,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<TabSettingsAdapter.VH>() {

        inner class VH(val binding: ItemTabSettingsRowBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            ItemTabSettingsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun getItemCount() = tabs.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val tab = tabs[position]
            with(holder.binding) {
                // Label field
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

                // Left icon: render via Material Symbols font (same approach as the picker).
                // Typeface is cached after the first load so subsequent binds are synchronous.
                val cp = tab.iconCodepoint
                // Capture context on the main thread before any coroutine switch.
                val ctx: Context = requireContext()
                val iconColor = ctx
                    .getAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
                tabRowIconView.setTextColor(iconColor)
                if (cp != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val tf = withContext(Dispatchers.IO) {
                            MaterialSymbolsHelper.getTypeface(ctx.applicationContext)
                        }
                        tabRowIconView.typeface = tf
                        tabRowIconView.text = String(Character.toChars(cp))
                    }
                } else {
                    tabRowIconView.text = ""
                }

                tabRowPickWidget.setOnClickListener { onPickWidget(holder.bindingAdapterPosition) }
                tabRowPickIcon.setOnClickListener { onPickIcon(holder.bindingAdapterPosition) }
                tabRowDelete.setOnClickListener { onDelete(holder.bindingAdapterPosition) }

                tabRowDragHandle.setOnLongClickListener {
                    itemTouchHelper.startDrag(holder)
                    true
                }
            }
        }
    }
}

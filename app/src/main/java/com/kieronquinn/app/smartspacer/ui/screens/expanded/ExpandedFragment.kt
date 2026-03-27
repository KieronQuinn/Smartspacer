package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.app.Activity
import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardDismissCallback
import android.app.PendingIntent
import android.appwidget.AppWidgetProviderInfo
import android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurProvider
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.databinding.FragmentExpandedBinding
import com.kieronquinn.app.smartspacer.databinding.SmartspaceExpandedLongPressPopupBinding
import com.kieronquinn.app.smartspacer.databinding.SmartspaceExpandedLongPressPopupCustomWidgetBinding
import com.kieronquinn.app.smartspacer.databinding.SmartspaceExpandedLongPressPopupWidgetBinding
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.model.doodle.DoodleImage
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.model.expanded.ExpandedTabConfig
import com.kieronquinn.app.smartspacer.model.expanded.NavItemDisplayMode
import com.kieronquinn.app.smartspacer.repositories.ExpandedTabRepository
import com.kieronquinn.app.smartspacer.ui.screens.expanded.BaseExpandedAdapter.ExpandedAdapterListener
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.sdk.client.views.SmartspacerView
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_ABOUT_INTENT
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_FEEDBACK_INTENT
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget.Companion.FEATURE_WEATHER
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.shouldExcludeFromSmartspacer
import com.kieronquinn.app.smartspacer.ui.activities.ExpandedActivity
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.activities.OverlayTrampolineActivity
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession.State
import com.kieronquinn.app.smartspacer.utils.extensions.MaterialSymbolsHelper
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.getContrastColor
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.isActivityCompat
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.utils.extensions.overrideRippleColor
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import com.kieronquinn.app.smartspacer.sdk.client.R as SDKR

class ExpandedFragment : BoundFragment<FragmentExpandedBinding>(
    FragmentExpandedBinding::inflate
), SmartspaceTargetInteractionListener, ExpandedAdapterListener, ProvidesBack {

    companion object {
        private const val MIN_SWIPE_DELAY = 250L
        private const val EXTRA_OPEN_ACTION = "open_action"

        private val COMPONENT_EXPANDED = ComponentName(
            BuildConfig.APPLICATION_ID,
            "${BuildConfig.APPLICATION_ID}.ui.activities.ExportedExpandedActivity"
        )

        fun createOpenTargetIntent(targetId: String): Intent {
            val action = OpenFromOverlayAction.OpenTarget(targetId)
            return Intent().apply {
                component = COMPONENT_EXPANDED
                putExtra(EXTRA_OPEN_ACTION, action)
            }
        }

        fun createOpenTargetUriCompatibleIntent(launchIntent: Intent?): Intent? {
            if (launchIntent?.component != COMPONENT_EXPANDED) return null
            val targetId = launchIntent.getParcelableExtraCompat(
                EXTRA_OPEN_ACTION, OpenFromOverlayAction.OpenTarget::class.java
            )?.id ?: return null
            return Intent().apply {
                component = ComponentName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.ui.activities.ExportedExpandedActivity"
                )
                putExtra("open_target", targetId)
            }
        }
    }

    private val isOverlay by lazy {
        ExpandedActivity.isOverlay(requireActivity() as ExpandedActivity)
    }
    private val isMinusOne by lazy {
        ExpandedActivity.isMinusOne(requireActivity() as ExpandedActivity)
    }
    private val uid by lazy {
        ExpandedActivity.getUid(requireActivity() as ExpandedActivity)
    }
    private val sessionId by lazy {
        when {
            isMinusOne -> "minusOne"
            isOverlay -> "overlay"
            else -> "expanded"
        }
    }

    private val viewModel by viewModel<ExpandedViewModel> {
        parametersOf("${sessionId}_$uid")
    }
    private val wallpaperRepository by inject<WallpaperRepository>()
    private val blurProvider by inject<BlurProvider>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val expandedRepository by inject<ExpandedRepository>()
    private val tabRepository by inject<ExpandedTabRepository>()
    private val atAGlanceRepository by inject<AtAGlanceRepository>()
    private val googleWeatherRepository by inject<GoogleWeatherRepository>()

    private var lastSwipe: Long? = null
    private var popup: Balloon? = null
    private var topInset = 0
    private var currentTabIndex = 0
    private val loadedTabIndices = mutableSetOf<Int>()

    /** All non-weather Smartspace targets available to page through in the header pill. */
    private var headerTargets: List<Item.Target> = emptyList()
    /** Index into [headerTargets] of the target currently shown in the header pill. */
    private var currentHeaderIndex = 0
    private val headerPagerAdapter by lazy { HeaderTargetAdapter() }

    override val applyTransitions = false

    private val widgetBindResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onWidgetBindResult(widgetConfigureResult, it.resultCode == Activity.RESULT_OK)
    }
    private val widgetConfigureResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.onWidgetConfigureResult(it.resultCode == Activity.RESULT_OK)
    }

    private val isDark = runBlocking {
        wallpaperRepository.homescreenWallpaperDarkTextColour.first()
    }
    private val backgroundColour by lazy { monet.getBackgroundColor(requireContext()) }
    private val keyguardManager by lazy {
        requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val theme = if (isDark) R.style.Theme_Smartspacer_Wallpaper_Dark
        else R.style.Theme_Smartspacer_Wallpaper_Light
        return inflater.cloneInContext(ContextThemeWrapper(requireContext(), theme))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowCompat.getInsetsController(requireActivity().window, view).run {
            isAppearanceLightNavigationBars = isDark
            isAppearanceLightStatusBars = isDark
        }
        if (isMinusOne) setBlurEnabled(true)
        setupLoading()
        setupState()
        setupMonet()
        setupInsets()
        setupUnlock()
        setupOverlaySwipe()
        setupDisabledButton()
        setupClose()
        setupMenuButton()
        setupOpenAppButton()
        setupTabs()
        setupHeaderSwipe()
        viewModel.setup(isOverlay)
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    private fun setupLoading() {
        (binding.expandedLoading.drawable as? AnimatedVectorDrawable)?.start()
    }

    private fun setupMonet() {
        reapplyColors()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun reapplyColors() {
        val ctx = requireContext()

        // Unlock / permission UI
        binding.expandedUnlockContainer.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColor(ctx))
        binding.expandedUnlock.overrideRippleColor(monet.getAccentColor(ctx))
        binding.expandedUnlock.iconTint =
            ColorStateList.valueOf(monet.getAccentColor(ctx))
        binding.expandedDisabledButton.backgroundTintList = ColorStateList.valueOf(
            ctx.getAttrColor(androidx.appcompat.R.attr.colorPrimary)
        )
        binding.expandedPermission.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColor(ctx))

        // Pill
        val primaryContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorPrimaryContainer)
        val onPrimaryContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
        binding.expandedHeaderPill.setCardBackgroundColor(primaryContainer)
        binding.expandedHeaderPill.strokeColor =
            ctx.getAttrColor(com.google.android.material.R.attr.colorOutline)

        // Menu button inside pill
        binding.expandedHeaderMenu.iconTint = ColorStateList.valueOf(onPrimaryContainer)

        // Weather cookie (SquircleFrameLayout uses android:background ColorDrawable — set directly)
        binding.expandedHeaderWeather.setBackgroundColor(primaryContainer)
        binding.expandedHeaderWeatherTemp.setTextColor(onPrimaryContainer)

        // Nav pill (tab scroll container)
        val surfaceContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorSurfaceContainer)
        binding.expandedTabScrollPill.setCardBackgroundColor(surfaceContainer)

        // FAB — P-90 (primary-fixed) background, P-10 (on-primary-fixed) icon.
        // On API 31+ use the system Monet palette directly (always correct).
        // Below API 31 fall back to MonetCompat's shade lookup.
        val primaryFixed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ctx.getColor(android.R.color.system_accent1_100)
        } else {
            ctx.getAttrColor(com.google.android.material.R.attr.colorPrimaryContainer)
        }
        val onPrimaryFixed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ctx.getColor(android.R.color.system_accent1_900)
        } else {
            ctx.getAttrColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
        }
        binding.expandedTabOpenApp.backgroundTintList = ColorStateList.valueOf(primaryFixed)
        binding.expandedTabOpenApp.iconTint = ColorStateList.valueOf(onPrimaryFixed)

        // Empty-state label
        val onSurfaceVariant = ctx.getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        binding.expandedEmptyLabel.setTextColor(onSurfaceVariant)

        // Tab buttons — re-select current tab so selected/unselected colours refresh
        selectTab(currentTabIndex, animate = false)
    }

    private fun setupInsets() {
        binding.expandedUnlockContainer.onApplyInsets { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
        }
        binding.expandedTabNavigation.onApplyInsets { view, insets ->
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = navBarBottom + (9.16f * resources.displayMetrics.density).toInt()
            }
        }
        binding.root.onApplyInsets { view, insets ->
            topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            viewModel.setTopInset(topInset)
            // Push all content below the status bar so the glance pill is never hidden behind it.
            view.updatePadding(top = topInset)
        }
    }

    private fun setupMenuButton() = viewLifecycleOwner.whenResumed {
        binding.expandedHeaderMenu.onClicked().collect {
            if (!isOverlay && !isMinusOne) {
                findNavController().navigate(R.id.action_expandedFragment_to_expandedTabSettingsFragment)
            } else {
                launchOverlayAction(OpenFromOverlayAction.AddWidget(0))
            }
        }
    }

    private fun setupOpenAppButton() {
        binding.expandedTabOpenApp.setOnClickListener {
            try {
                val intent = requireContext().packageManager
                    .getLaunchIntentForPackage("me.ash.reader")
                    ?: throw IllegalStateException("ReadYou (me.ash.reader) is not installed")
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTabs() {
        whenResumed {
            tabRepository.tabs.collect { tabs -> buildTabUI(tabs) }
        }
        // Re-apply button icons/text whenever the display mode changes, even if the tab list
        // itself hasn't changed (StateFlow wouldn't re-emit an identical tab list).
        whenResumed {
            tabRepository.navItemDisplayMode.drop(1).collect { selectTab(currentTabIndex, animate = false) }
        }
    }

    private fun buildTabUI(tabs: List<ExpandedTabConfig>) {
        loadedTabIndices.clear()
        currentTabIndex = 0
        if (tabs.isEmpty()) {
            binding.expandedWidgetFlipper.isVisible = false
            binding.expandedEmptyLabel.isVisible = true
            binding.expandedTabScrollPill.isVisible = false
            return
        }
        binding.expandedWidgetFlipper.isVisible = true
        binding.expandedEmptyLabel.isVisible = false
        binding.expandedTabScrollPill.isVisible = true

        binding.expandedWidgetFlipper.removeAllViews()
        tabs.forEach { _ ->
            binding.expandedWidgetFlipper.addView(
                FrameLayout(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            )
        }

        val inflater = LayoutInflater.from(requireContext())
        binding.expandedTabButtons.removeAllViews()
        tabs.forEachIndexed { index, _ ->
            val itemView = inflater.inflate(
                R.layout.item_expanded_nav_item,
                binding.expandedTabButtons,
                false
            )
            itemView.setOnClickListener { selectTab(index) }
            binding.expandedTabButtons.addView(itemView)
        }
        // Remove trailing gap from last item so the pill wraps content tightly (CSS gap
        // only adds space between items, marginEnd adds it after the last one too).
        (binding.expandedTabButtons.getChildAt(tabs.size - 1)?.layoutParams
                as? LinearLayout.LayoutParams)?.marginEnd = 0
        selectTab(0, animate = false)
    }

    private fun setupHeaderSwipe() {
        // ViewPager2 handles finger-tracking and page snapping directly.
        // The pill's SwipeDetectingCardView still fires requestDisallowInterceptTouchEvent on
        // ACTION_DOWN to guard against SlidingPanelLayout in the overlay context, but its own
        // fling-intercept is disabled (onHorizontalSwipe = null) so it never cancels the pager.
        binding.expandedHeaderPill.onHorizontalSwipe = null

        // The ViewPager2 now spans the full pill width (constrained to parent end) so the pill's
        // rounded corners clip content naturally.  The menu button and weather cookie float on top
        // in z-order (they appear later in the ConstraintLayout XML), so content slides under them.
        // clipToPadding=false + small right padding lets the edge of the next page peek into view
        // during a swipe, matching the Figma "peek" design (Pages Container = 240dp, page = 232dp
        // → 8dp visible peek).
        binding.expandedHeaderTarget.apply {
            clipToPadding = false
            offscreenPageLimit = 1
            // 8dp right padding → shows a sliver of the next page while swiping
            setPadding(0, 0, 8.dp, 0)
        }
        // Allow pages to draw into the padding area so the peek effect works
        (binding.expandedHeaderTarget.parent as? android.view.ViewGroup)?.clipChildren = false

        // Composite transformer:
        //  1. 8dp gap between pages so they don't butt against each other while peeking
        //  2. Scale + fade pages as they move off-centre (Figma "feel" — subtle depth)
        val transformer = CompositePageTransformer()
        transformer.addTransformer(MarginPageTransformer(8.dp))
        transformer.addTransformer { page, position ->
            val absPos = Math.abs(position)
            page.alpha = 1f - (absPos * 0.25f)
            page.scaleX = 1f - (absPos * 0.05f)
            page.scaleY = 1f - (absPos * 0.05f)
        }
        binding.expandedHeaderTarget.setPageTransformer(transformer)

        binding.expandedHeaderTarget.adapter = headerPagerAdapter
        binding.expandedHeaderTarget.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentHeaderIndex = position
                    updateHeaderDots()
                }
            }
        )
    }

    private fun selectTab(index: Int, animate: Boolean = true) {
        val tabs = tabRepository.getTabs()
        if (index < 0 || index >= tabs.size) return
        val previousIndex = currentTabIndex
        currentTabIndex = index
        binding.expandedWidgetFlipper.displayedChild = index

        val ctx = requireContext()
        // M3 nav-bar tokens
        val secondaryContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorSecondaryContainer)
        val onSecondaryContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
        val primaryColor = ctx.getAttrColor(androidx.appcompat.R.attr.colorPrimary)
        val onPrimary = ctx.getAttrColor(com.google.android.material.R.attr.colorOnPrimary)
        val onSurfaceVariant = ctx.getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        val displayMode = tabRepository.getNavItemDisplayMode()
        val symbolTypeface = MaterialSymbolsHelper.getTypeface(ctx)

        // Active indicator pill — 41 dp tall, 20 dp corner radius (Figma node 212:809).
        fun pillBg(color: Int): GradientDrawable = GradientDrawable().apply {
            cornerRadius = 20.dp.toFloat()
            setColor(color)
        }

        for (i in 0 until binding.expandedTabButtons.childCount) {
            val itemView = binding.expandedTabButtons.getChildAt(i) ?: continue
            // Icon is a TextView so the Material Symbols font glyph renders immediately
            // without waiting for a layout pass (ImageView.configureBounds requires one).
            val iconView = itemView.findViewById<TextView>(R.id.nav_item_icon) ?: continue
            val label = itemView.findViewById<TextView>(R.id.nav_item_label) ?: continue
            val tab = tabs.getOrNull(i) ?: continue
            val isSelected = i == index
            val wasSelected = i == previousIndex && !isSelected

            fun setIcon(cp: Int, color: Int) {
                iconView.typeface = symbolTypeface
                iconView.setTextColor(color)
                iconView.text = String(Character.toChars(cp))
            }

            // Helpers for animated background alpha fade
            fun fadeBgIn(color: Int, cornerRadius: Float) {
                val bg = GradientDrawable().apply { this.cornerRadius = cornerRadius; setColor(color); alpha = 0 }
                itemView.background = bg
                ValueAnimator.ofInt(0, 255).apply {
                    duration = 150
                    interpolator = DecelerateInterpolator(1.5f)
                    addUpdateListener { bg.alpha = animatedValue as Int }
                    start()
                }
            }
            fun fadeBgOut(color: Int, cornerRadius: Float) {
                val bg = GradientDrawable().apply { this.cornerRadius = cornerRadius; setColor(color); alpha = 255 }
                itemView.background = bg
                ValueAnimator.ofInt(255, 0).apply {
                    duration = 150
                    interpolator = DecelerateInterpolator(1.5f)
                    addUpdateListener { bg.alpha = animatedValue as Int }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(a: android.animation.Animator) { itemView.background = null }
                    })
                    start()
                }
            }

            when (displayMode) {
                NavItemDisplayMode.LABEL_ONLY -> {
                    // Reset any ICON_ONLY square sizing back to wrap_content
                    if (itemView.layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                        itemView.layoutParams = itemView.layoutParams.also { it.width = ViewGroup.LayoutParams.WRAP_CONTENT }
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                        (itemView as? LinearLayout)?.gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    iconView.visibility = View.GONE
                    label.visibility = View.VISIBLE
                    label.text = tab.label
                    val pillRadius = 20.dp.toFloat()
                    if (isSelected) {
                        label.setTextColor(onPrimary)
                        if (animate && wasSelected.not() && itemView.background == null) {
                            fadeBgIn(primaryColor, pillRadius)
                        } else {
                            itemView.background = pillBg(primaryColor)
                        }
                    } else {
                        label.setTextColor(onSurfaceVariant)
                        if (animate && wasSelected && itemView.background != null) {
                            fadeBgOut(primaryColor, pillRadius)
                        } else {
                            itemView.background = null
                        }
                    }
                }

                NavItemDisplayMode.ICON_ONLY -> {
                    // Force a perfect circle: item is always 41×41 dp, no padding, icon centred
                    val size = 41.dp
                    if (itemView.layoutParams.width != size) {
                        itemView.layoutParams = itemView.layoutParams.also { it.width = size }
                        itemView.setPadding(0, 0, 0, 0)
                        (itemView as? LinearLayout)?.gravity = android.view.Gravity.CENTER
                        (iconView.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = 0
                    }
                    label.visibility = View.GONE
                    val cp = tab.iconCodepoint
                    if (cp != null) {
                        setIcon(cp, if (isSelected) onSecondaryContainer else onSurfaceVariant)
                        iconView.visibility = View.VISIBLE
                    } else {
                        iconView.visibility = View.GONE
                    }
                    val circleRadius = 100.dp.toFloat()
                    if (isSelected) {
                        if (animate && wasSelected.not() && itemView.background == null) {
                            fadeBgIn(secondaryContainer, circleRadius)
                        } else {
                            itemView.background = GradientDrawable().apply { cornerRadius = circleRadius; setColor(secondaryContainer) }
                        }
                    } else {
                        if (animate && wasSelected && itemView.background != null) {
                            fadeBgOut(secondaryContainer, circleRadius)
                        } else {
                            itemView.background = null
                        }
                    }
                }

                NavItemDisplayMode.ICON_AND_LABEL -> {
                    // Reset any ICON_ONLY square sizing back to wrap_content
                    if (itemView.layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                        itemView.layoutParams = itemView.layoutParams.also { it.width = ViewGroup.LayoutParams.WRAP_CONTENT }
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                        (itemView as? LinearLayout)?.gravity = android.view.Gravity.CENTER_VERTICAL
                        (iconView.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = 4.dp
                    }
                    label.visibility = View.VISIBLE
                    label.text = tab.label
                    val cp = tab.iconCodepoint

                    if (isSelected) {
                        itemView.background = pillBg(primaryColor)
                        label.setTextColor(onPrimary)

                        if (animate && cp != null && itemView.width > 0) {
                            // Set glyph before measuring — TextView renders immediately.
                            setIcon(cp, onPrimary)
                            iconView.alpha = 0f
                            iconView.visibility = View.VISIBLE
                            itemView.measure(
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(41.dp, View.MeasureSpec.EXACTLY)
                            )
                            val expandedWidth = itemView.measuredWidth
                            // Bounce-grow the width; fade icon in simultaneously so it
                            // appears to emerge as the pill expands.
                            animateItemWidth(itemView, itemView.width, expandedWidth, overshoot = true)
                            iconView.animate().cancel()
                            iconView.animate().alpha(1f).setDuration(200).setListener(null).start()
                        } else {
                            if (cp != null) {
                                setIcon(cp, onPrimary)
                                iconView.alpha = 1f
                                iconView.visibility = View.VISIBLE
                            } else {
                                iconView.visibility = View.GONE
                            }
                        }
                    } else {
                        itemView.background = null
                        label.setTextColor(onSurfaceVariant)

                        if (animate && wasSelected && itemView.width > 0) {
                            iconView.animate().cancel()
                            if (iconView.visibility == View.VISIBLE) {
                                iconView.animate().alpha(0f).setDuration(80)
                                    .setListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(a: android.animation.Animator) {
                                            iconView.visibility = View.GONE
                                            iconView.alpha = 1f
                                            collapseItemWidth(itemView)
                                        }
                                        override fun onAnimationCancel(a: android.animation.Animator) {
                                            iconView.visibility = View.GONE
                                            iconView.alpha = 1f
                                        }
                                    }).start()
                            } else {
                                collapseItemWidth(itemView)
                            }
                        } else {
                            iconView.visibility = View.GONE
                            iconView.alpha = 1f
                        }
                    }
                }
            }
        }

        if (!loadedTabIndices.contains(index)) {
            loadedTabIndices.add(index)
            loadWidgetForTab(index, tabs[index].appWidgetId)
        }
    }

    private fun collapseItemWidth(item: View) {
        item.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(41.dp, View.MeasureSpec.EXACTLY)
        )
        animateItemWidth(item, item.width, item.measuredWidth, overshoot = false)
    }

    /**
     * Animates [view] width from [from] → [to] px.
     * overshoot=true → OvershootInterpolator (spring-bounce expand, 260 ms).
     * overshoot=false → DecelerateInterpolator (smooth collapse, 160 ms).
     * [onEnd] fires once the animation settles at [to].
     */
    private fun animateItemWidth(view: View, from: Int, to: Int, overshoot: Boolean, onEnd: (() -> Unit)? = null) {
        if (from == to) { onEnd?.invoke(); return }
        view.layoutParams = view.layoutParams.also { it.width = from }
        ValueAnimator.ofInt(from, to).apply {
            duration = if (overshoot) 260L else 160L
            interpolator = if (overshoot) OvershootInterpolator(1.5f) else DecelerateInterpolator(1.5f)
            addUpdateListener { view.layoutParams = view.layoutParams.also { lp -> lp.width = animatedValue as Int } }
            if (onEnd != null) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: android.animation.Animator) { onEnd() }
                })
            }
            start()
        }
    }

    private fun loadWidgetForTab(tabIndex: Int, appWidgetId: Int) {
        val container = binding.expandedWidgetFlipper.getChildAt(tabIndex) as? FrameLayout ?: return
        val state = viewModel.state.value as? State.Loaded ?: return
        val widgetItem = state.items.filterIsInstance<Item.Widget>()
            .firstOrNull { it.appWidgetId == appWidgetId } ?: return

        val availableWidth = binding.expandedWidgetFlipper.measuredWidth
            .takeIf { it > 0 } ?: resources.displayMetrics.widthPixels

        val hostView = expandedRepository.createHost(requireContext(), availableWidth, widgetItem, sessionId, this)
        // If the cached view is still attached to a stale container (left over after
        // buildTabUI replaced all FrameLayouts), detach it before re-parenting.
        (hostView.parent as? android.view.ViewGroup)?.removeView(hostView)
        container.removeAllViews()
        container.addView(hostView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        // After layout, tell the widget provider the full available size so it renders
        // at the correct height instead of its minimum span height.
        binding.expandedWidgetFlipper.post {
            val fullHeight = binding.expandedWidgetFlipper.height.takeIf { it > 0 } ?: return@post
            val fullWidth = binding.expandedWidgetFlipper.width.takeIf { it > 0 } ?: availableWidth
            hostView.updateSizeIfNeeded(fullWidth.toFloat(), fullHeight.toFloat())
        }
    }

    // ── State ─────────────────────────────────────────────────────────────

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed { viewModel.state.collect { handleState(it) } }
        whenResumed { viewModel.rawPageTargets.collect { updateHeaderTargets(it) } }
    }

    private fun handleState(state: State) {
        val isLoaded = state is State.Loaded
        binding.expandedLoading.isVisible = state is State.Loading
        binding.expandedHeaderPill.isVisible = isLoaded
        binding.expandedTabNavigation.isVisible = isLoaded
        binding.expandedDisabled.isVisible = state is State.Disabled
        binding.expandedPermission.isVisible = state is State.PermissionRequired
        if (state is State.Loaded) {
            binding.expandedUnlockContainer.isVisible = state.isLocked && !isOverlay && !isMinusOne
            setStatusBarLight(state.lightStatusIcons)
            updateHeaderTarget(state.items)
            updateWeatherCookie(state.items)
            // Retry widget load for current tab if container still empty
            val tabs = tabRepository.getTabs()
            if (tabs.isNotEmpty()) {
                val container = binding.expandedWidgetFlipper.getChildAt(currentTabIndex) as? FrameLayout
                if (container != null && container.childCount == 0) {
                    loadedTabIndices.remove(currentTabIndex)
                    loadWidgetForTab(currentTabIndex, tabs[currentTabIndex].appWidgetId)
                }
            }
        } else {
            binding.expandedUnlockContainer.isVisible = false
        }
    }

    private fun updateHeaderTarget(items: List<Item>) {
        // No-op: header paging is driven by updateHeaderTargets(rawPageTargets).
    }

    /**
     * Rebuilds [headerTargets] from the raw session page list (all targets in session order,
     * including blank complication targets, excluding weather).  This mirrors exactly what the
     * regular Smartspacer widget pages through.
     */
    private fun updateHeaderTargets(rawTargets: List<SmartspaceTarget>) {
        val targets = rawTargets
            .filter { it.featureType != FEATURE_WEATHER }
            .map { target -> Item.Target(target, null, false, applyShadow = false, isDark = isDark) }
        headerTargets = targets
        if (currentHeaderIndex >= targets.size) currentHeaderIndex = 0
        headerPagerAdapter.submitTargets(targets)
        binding.expandedHeaderTarget.setCurrentItem(currentHeaderIndex, false)
        updateHeaderDots()
    }

    /** Rebuilds the dot-indicator row to reflect the current page and total count. */
    private fun updateHeaderDots() {
        val dots = binding.expandedHeaderDots
        val count = headerTargets.size
        dots.isVisible = count > 1
        dots.removeAllViews()
        if (!dots.isVisible) return
        val sizePx = resources.getDimensionPixelSize(R.dimen.header_dot_size)
        val gapPx  = resources.getDimensionPixelSize(R.dimen.header_dot_gap)
        // Figma: dots use sys/dark/on-primary-container (they sit on the primary-container pill)
        val dotColor = requireContext()
            .getAttrColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
        repeat(count) { i ->
            val dot = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(dotColor)
                }
                alpha = if (i == currentHeaderIndex) 1f else 0.3f
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    if (i > 0) marginStart = gapPx
                }
            }
            dots.addView(dot)
        }
    }

    private fun updateWeatherCookie(items: List<Item>) {
        // 0. Highest priority: At-a-Glance weather state (available when Shizuku is active and
        //    the AtAGlanceTarget is configured). Weather icons are the only ones that carry a
        //    non-null contentDescription in the At-a-Glance widget view hierarchy, so that flag
        //    is used to identify the weather state among all parsed At-a-Glance states.
        val atAGlanceWeather = atAGlanceRepository.getStates()
            .firstOrNull { !it.iconContentDescription.isNullOrEmpty() }
        if (atAGlanceWeather != null) {
            try {
                val bitmapIcon = android.graphics.drawable.Icon.createWithBitmap(atAGlanceWeather.icon)
                binding.expandedHeaderWeatherIcon.setImageIcon(bitmapIcon)
                binding.expandedHeaderWeather.isVisible = true
                val temp = atAGlanceWeather.subtitle.toString().takeIf { it.isNotBlank() }
                    ?: atAGlanceWeather.title.toString().takeIf { it.isNotBlank() }
                binding.expandedHeaderWeatherTemp.isVisible = !temp.isNullOrBlank()
                binding.expandedHeaderWeatherTemp.text = temp ?: ""
            } catch (e: Exception) {
                binding.expandedHeaderWeather.isVisible = false
            }
            return
        }

        // 1. Dedicated FEATURE_WEATHER target (e.g. Google At-a-Glance weather target).
        val weatherTarget = items.filterIsInstance<Item.Target>()
            .firstOrNull { it.target.featureType == FEATURE_WEATHER }
        val actionFromTarget = weatherTarget?.target?.run { headerAction ?: baseAction }

        // 2. Fall back to the weather complication action — the GoogleWeatherComplication plugin
        //    produces a SmartspaceAction (with a weather-condition icon bitmap) that surfaces in
        //    the complications list rather than as a dedicated target.  The first action that
        //    carries a non-null icon is treated as the weather action.
        val actionFromComp = if (actionFromTarget == null) {
            items.filterIsInstance<Item.Complications>()
                .firstOrNull()
                ?.complications
                ?.complications
                ?.filterIsInstance<ExpandedSession.Complications.Complication.Action>()
                ?.firstOrNull { it.smartspaceAction.icon != null }
                ?.smartspaceAction
        } else null

        // 3. Last resort: read the TodayState from GoogleWeatherRepository directly.
        //    This fires even when no GoogleWeatherComplication is configured, as long as
        //    the Google Weather widget (GoogleWeatherWidget provider) is bound somewhere.
        val todayState = if (actionFromTarget == null && actionFromComp == null) {
            googleWeatherRepository.getTodayState()
        } else null

        if (todayState != null) {
            try {
                val bitmapIcon = android.graphics.drawable.Icon.createWithBitmap(todayState.icon)
                binding.expandedHeaderWeatherIcon.setImageIcon(bitmapIcon)
                binding.expandedHeaderWeather.isVisible = true
                binding.expandedHeaderWeatherTemp.isVisible = todayState.temperature.isNotBlank()
                binding.expandedHeaderWeatherTemp.text = todayState.temperature
            } catch (e: Exception) {
                binding.expandedHeaderWeather.isVisible = false
            }
            return
        }

        val action = actionFromTarget ?: actionFromComp

        val icon = action?.icon
        if (icon == null) { binding.expandedHeaderWeather.isVisible = false; return }
        try {
            binding.expandedHeaderWeatherIcon.setImageIcon(icon)
            binding.expandedHeaderWeather.isVisible = true
        } catch (e: Exception) {
            binding.expandedHeaderWeather.isVisible = false
            return
        }
        // Temperature may live in subtitle or title — prefer subtitle as it is the
        // supplemental text field used by the weather complication template.
        val temp = action.subtitle?.toString()?.takeIf { it.isNotBlank() }
            ?: action.title.takeIf { it.isNotBlank() }
        binding.expandedHeaderWeatherTemp.isVisible = !temp.isNullOrBlank()
        binding.expandedHeaderWeatherTemp.text = temp ?: ""
    }

    private fun setStatusBarLight(enabled: Boolean) {
        WindowCompat.getInsetsController(requireActivity().window, requireView())
            .isAppearanceLightStatusBars = enabled
    }

    // ── Blur / background ─────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        if (!isMinusOne) setBlurEnabled(true)
    }

    override fun onPause() {
        if (!isMinusOne) setBlurEnabled(false)
        super.onPause()
        viewModel.onPause()
    }

    private fun setBlurEnabled(enabled: Boolean) {
        if (isOverlay) return
        // Always use a fully opaque background for all non-overlay modes so the
        // Monet surface colour is never transparent or semi-transparent.
        val bg = monet.getBackgroundColor(requireContext())
        binding.root.setBackgroundColor(
            ColorUtils.setAlphaComponent(bg, if (enabled) 255 else 0)
        )
        // Additionally drive the window blur when BLUR mode is selected.
        if (settingsRepository.expandedBackground.getSync() == ExpandedBackground.BLUR) {
            blurProvider.applyBlurToWindow(requireActivity().window, if (enabled) 1f else 0f)
        }
    }

    // ── Keyguard ──────────────────────────────────────────────────────────

    private fun setupUnlock() = viewLifecycleOwner.whenResumed {
        binding.expandedUnlock.onClicked().collect { unlockAndLaunch(null) }
    }

    private fun setupOverlaySwipe() = viewLifecycleOwner.whenResumed {
        // Only track overlay drag events when running as the overlay; in the standalone
        // expanded activity these events must not pollute the long-press swipe guard.
        if (!isOverlay) return@whenResumed
        viewModel.overlayDrag.collect { lastSwipe = System.currentTimeMillis(); popup?.dismiss(); popup = null }
    }

    private fun setupDisabledButton() = viewLifecycleOwner.whenResumed {
        binding.expandedDisabledButton.onClicked().collect {
            startActivity(Intent(requireContext(), MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartspacer://expanded")
                putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun setupClose() = viewLifecycleOwner.whenResumed {
        viewModel.exitBus.collect {
            if (it && !isOverlay) finishExpanded()
        }
    }

    /** Finishes the expanded activity without a slide-away animation to avoid the translucent
     *  window "overlay on top of home" visual glitch. */
    private fun finishExpanded() {
        requireActivity().overridePendingTransition(0, 0)
        requireActivity().finishAndRemoveTask()
    }

    // ── ProvidesBack ──────────────────────────────────────────────────────

    /** Always intercept back so we control exactly how the activity is dismissed. */
    override fun interceptBack() = !isOverlay

    override fun onBackPressed(): Boolean {
        if (isOverlay) return false
        finishExpanded()
        return true
    }

    private fun unlockAndLaunch(intent: Intent?) = unlockAndInvoke {
        try {
            startActivity(intent?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } ?: return@unlockAndInvoke)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), SDKR.string.smartspace_long_press_popup_failed_to_launch, Toast.LENGTH_LONG).show()
        }
    }

    private fun unlockAndInvoke(block: () -> Unit) {
        if (!isAdded) return
        if (!keyguardManager.isKeyguardLocked) { block(); return }
        keyguardManager.requestDismissKeyguard(requireActivity(), object : KeyguardDismissCallback() {
            override fun onDismissSucceeded() { super.onDismissSucceeded(); block() }
        })
    }

    // ── SmartspaceTargetInteractionListener ───────────────────────────────

    override fun onInteraction(target: SmartspaceTarget, actionId: String?) =
        viewModel.onTargetInteraction(target, actionId)

    override fun onLongPress(target: SmartspaceTarget): Boolean {
        val canDismiss = target.canBeDismissed && target.featureType != SmartspaceTarget.FEATURE_WEATHER
        val aboutIntent = target.baseAction?.extras?.getParcelableCompat(KEY_EXTRA_ABOUT_INTENT, Intent::class.java)?.takeIf { !it.shouldExcludeFromSmartspacer() }
        val feedbackIntent = target.baseAction?.extras?.getParcelableCompat(KEY_EXTRA_FEEDBACK_INTENT, Intent::class.java)?.takeIf { !it.shouldExcludeFromSmartspacer() }
        if (!canDismiss && aboutIntent == null && feedbackIntent == null) return false
        return showTargetPopup(binding.expandedHeaderTarget, target, canDismiss, aboutIntent, feedbackIntent)
    }

    override fun launch(unlock: Boolean, block: () -> Unit) = if (unlock) unlockAndInvoke(block) else block()
    override fun shouldTrampolineLaunches() = isOverlay
    override fun trampolineLaunch(view: View, pendingIntent: PendingIntent) =
        OverlayTrampolineActivity.trampoline(view, requireContext(), pendingIntent)

    override fun onSearchLensClicked(searchApp: SearchApp) = unlockAndLaunch(
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google://lens")
            component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensExportedActivity")
            putExtra("LensHomescreenShortcut", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )

    override fun onSearchMicClicked(searchApp: SearchApp) {
        val block = { startActivity(Intent(Intent.ACTION_VOICE_COMMAND).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
        if (searchApp.requiresUnlock) unlockAndInvoke(block) else block()
    }

    override fun onDoodleClicked(doodleImage: DoodleImage) = unlockAndInvoke {
        startActivity(Intent(Intent.ACTION_VIEW).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); data = Uri.parse(doodleImage.searchUrl ?: return@apply) })
    }

    override fun onSearchBoxClicked(searchApp: SearchApp) = unlockAndInvoke {
        try { startActivity(searchApp.launchIntent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
        catch (e: Exception) { requireContext().packageManager.getLaunchIntentForPackage(searchApp.packageName)?.let { startActivity(it) } }
    }

    override fun onConfigureWidgetClicked(info: AppWidgetProviderInfo, id: String?, config: CustomExpandedAppWidgetConfig?) {
        if (isOverlay || isMinusOne) launchOverlayAction(OpenFromOverlayAction.ConfigureWidget(info, id, config, 0))
        else unlockAndInvoke { viewModel.onConfigureWidgetClicked(widgetBindResult, widgetConfigureResult, info, id, config) }
    }

    override fun onAddWidgetClicked() {
        if (isOverlay || isMinusOne) launchOverlayAction(OpenFromOverlayAction.AddWidget(0))
        else unlockAndInvoke { viewModel.onAddWidgetClicked() }
    }

    override fun onShortcutClicked(shortcut: Shortcut) {
        if (shortcut.pendingIntent?.isActivityCompat() == true) viewModel.onShortcutClicked(requireContext(), shortcut)
        else unlockAndInvoke { viewModel.onShortcutClicked(requireContext(), shortcut) }
    }

    override fun onAppShortcutClicked(appShortcut: AppShortcut) =
        unlockAndInvoke { viewModel.onAppShortcutClicked(appShortcut) }

    override fun onWidgetLongClicked(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, appWidgetId: Int?): Boolean {
        if (appWidgetId == null) return false
        lastSwipe?.let { if (System.currentTimeMillis() - it < MIN_SWIPE_DELAY) return false }
        val pv = SmartspaceExpandedLongPressPopupWidgetBinding.inflate(layoutInflater)
        val bg = requireContext().getAttrColor(android.R.attr.colorBackground)
        val fg = bg.getContrastColor()
        buildBalloon(pv.root).also { b ->
            b.showAlignBottom(binding.expandedHeaderPill)
            pv.expandedLongPressPopupReset.setTextColor(fg)
            pv.expandedLongPressPopupReset.iconTint = ColorStateList.valueOf(fg)
            pv.expandedLongPressPopupReset.setOnClickListener { b.dismiss(); unlockAndInvoke { viewModel.onAppWidgetReset(appWidgetId) } }
            popup = b
        }
        return true
    }

    override fun onWidgetDeleteClicked(widget: Item.RemovedWidget) {
        viewModel.onDeleteCustomWidget(widget.appWidgetId ?: return)
    }

    override fun onCustomWidgetLongClicked(view: View, widget: Item.Widget): Boolean {
        lastSwipe?.let { if (System.currentTimeMillis() - it < MIN_SWIPE_DELAY) return false }
        val pv = SmartspaceExpandedLongPressPopupCustomWidgetBinding.inflate(layoutInflater)
        val bg = requireContext().getAttrColor(android.R.attr.colorBackground)
        val fg = bg.getContrastColor()
        buildBalloon(pv.root).also { b ->
            b.showAlignBottom(view)
            listOf(pv.expandedLongPressPopupDelete, pv.expandedLongPressPopupOptions, pv.expandedLongPressPopupRearrange).forEach {
                it.setTextColor(fg); it.iconTint = ColorStateList.valueOf(fg)
            }
            pv.expandedLongPressPopupDelete.setOnClickListener { b.dismiss(); unlockAndInvoke { viewModel.onDeleteCustomWidget(widget.appWidgetId ?: return@unlockAndInvoke) } }
            pv.expandedLongPressPopupOptions.setOnClickListener {
                b.dismiss()
                val id = widget.appWidgetId ?: return@setOnClickListener
                val canRec = widget.provider.canReconfigure()
                if (isOverlay) launchOverlayAction(OpenFromOverlayAction.Options(0, id, canRec))
                else unlockAndInvoke { viewModel.onOptionsClicked(id, canRec) }
            }
            pv.expandedLongPressPopupRearrange.setOnClickListener {
                b.dismiss()
                if (isOverlay) launchOverlayAction(OpenFromOverlayAction.Rearrange(widget.appWidgetId ?: return@setOnClickListener))
                else unlockAndInvoke { viewModel.onRearrangeClicked() }
            }
            popup = b
        }
        return true
    }

    // ── Target popup ──────────────────────────────────────────────────────

    private fun showTargetPopup(anchor: View, target: SmartspaceTarget, canDismiss: Boolean, aboutIntent: Intent?, feedbackIntent: Intent?): Boolean {
        lastSwipe?.let { if (System.currentTimeMillis() - it < MIN_SWIPE_DELAY) return false }
        val pv = SmartspaceExpandedLongPressPopupBinding.inflate(layoutInflater)
        val bg = requireContext().getAttrColor(android.R.attr.colorBackground)
        val fg = bg.getContrastColor()
        buildBalloon(pv.root).also { b ->
            b.showAlignBottom(anchor)
            pv.smartspaceLongPressPopupAbout.isVisible = aboutIntent != null
            pv.smartspaceLongPressPopupAbout.setTextColor(fg); pv.smartspaceLongPressPopupAbout.iconTint = ColorStateList.valueOf(fg)
            pv.smartspaceLongPressPopupAbout.setOnClickListener { b.dismiss(); unlockAndLaunch(aboutIntent) }
            pv.smartspaceLongPressPopupFeedback.isVisible = feedbackIntent != null
            pv.smartspaceLongPressPopupFeedback.setTextColor(fg); pv.smartspaceLongPressPopupFeedback.iconTint = ColorStateList.valueOf(fg)
            pv.smartspaceLongPressPopupFeedback.setOnClickListener { b.dismiss(); unlockAndLaunch(feedbackIntent) }
            pv.smartspaceLongPressPopupDismiss.isVisible = canDismiss
            pv.smartspaceLongPressPopupDismiss.setTextColor(fg); pv.smartspaceLongPressPopupDismiss.iconTint = ColorStateList.valueOf(fg)
            pv.smartspaceLongPressPopupDismiss.setOnClickListener { b.dismiss(); viewModel.onTargetDismiss(target) }
            popup = b
        }
        return true
    }

    private fun buildBalloon(layout: View): Balloon {
        val bg = requireContext().getAttrColor(android.R.attr.colorBackground)
        return Balloon.Builder(requireContext())
            .setLayout(layout).setHeight(BalloonSizeSpec.WRAP)
            .setWidthResource(SDKR.dimen.smartspace_long_press_popup_width)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setBackgroundColor(bg).setArrowColor(bg)
            .setArrowSize(10).setArrowPosition(0.5f).setCornerRadius(16f)
            .setBalloonAnimation(BalloonAnimation.FADE).build()
    }

    // ── Overlay ───────────────────────────────────────────────────────────

    private fun launchOverlayAction(action: OpenFromOverlayAction) {
        unlockAndLaunch(ExpandedActivity.createExportedOverlayIntent(requireContext()).apply {
            putExtra(EXTRA_OPEN_ACTION, action)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    private fun AppWidgetProviderInfo.canReconfigure() =
        configure != null && widgetFeatures and WIDGET_FEATURE_RECONFIGURABLE != 0

    // ── Header pill pager adapter ──────────────────────────────────────────

    /**
     * Minimal RecyclerView.Adapter for the header pill [ViewPager2].
     * Each page is a [SmartspacerView] bound to one [Item.Target].
     */
    private inner class HeaderTargetAdapter
        : RecyclerView.Adapter<HeaderTargetAdapter.ViewHolder>() {

        private var targets: List<Item.Target> = emptyList()

        fun submitTargets(newTargets: List<Item.Target>) {
            targets = newTargets
            notifyDataSetChanged()
        }

        inner class ViewHolder(val sv: SmartspacerView) : RecyclerView.ViewHolder(sv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(SmartspacerView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(0, 0, 0, 0)
            })

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val target = targets[position]
            // Figma: target text uses sys/dark/on-primary-container on the primary-container pill
            val tintColour = requireContext()
                .getAttrColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
            holder.sv.setTarget(target.target, this@ExpandedFragment, tintColour, false)
        }

        override fun getItemCount() = targets.size
    }

    // ── Parcelable overlay actions ─────────────────────────────────────────

    sealed class OpenFromOverlayAction(open val scrollPosition: Int) : Parcelable {
        @Parcelize data class OpenTarget(val id: String) : OpenFromOverlayAction(0)
        @Parcelize data class ConfigureWidget(
            val info: AppWidgetProviderInfo,
            val id: String?,
            val config: CustomExpandedAppWidgetConfig?,
            override val scrollPosition: Int
        ) : OpenFromOverlayAction(scrollPosition)
        @Parcelize data class AddWidget(override val scrollPosition: Int) : OpenFromOverlayAction(scrollPosition)
        @Parcelize data class Rearrange(override val scrollPosition: Int) : OpenFromOverlayAction(scrollPosition)
        @Parcelize data class Options(
            override val scrollPosition: Int,
            val appWidgetId: Int,
            val canReconfigure: Boolean
        ) : OpenFromOverlayAction(scrollPosition)
    }
}

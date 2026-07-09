@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.app.Activity
import android.app.KeyguardManager
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.CompositePageTransformer
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnLayout
import androidx.core.view.doOnDetach
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
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.model.expanded.ExpandedTabConfig
import com.kieronquinn.app.smartspacer.model.expanded.NavItemDisplayMode
import com.kieronquinn.app.smartspacer.repositories.ExpandedTabRepository
import com.kieronquinn.app.smartspacer.ui.screens.expanded.BaseExpandedAdapter.ExpandedAdapterListener
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.sdk.client.views.SmartspacerView
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_ABOUT_INTENT
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_FEEDBACK_INTENT
import com.kieronquinn.app.smartspacer.sdk.utils.sendSafely
import com.kieronquinn.app.smartspacer.components.smartspace.compat.TargetMerger.Companion.BLANK_TARGET_PREFIX
import com.kieronquinn.app.smartspacer.components.smartspace.complications.AlarmComplication
import com.kieronquinn.app.smartspacer.repositories.ReadYouRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
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
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
        private const val EXTRA_NAVIGATE_TAB_SETTINGS = "nav_tab_settings"

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
    private val searchRepository by inject<SearchRepository>()
    private val expandedRepository by inject<ExpandedRepository>()
    private val tabRepository by inject<ExpandedTabRepository>()
    private val readYouRepository by inject<ReadYouRepository>()
    private val smartspaceRepository by inject<SmartspaceRepository>()


    private var lastSwipe: Long? = null
    private var popup: Balloon? = null
    private var topInset = 0
    private var currentTabIndex = 0
    private var isDoodleEnabled = false
    private val loadedTabIndices = mutableSetOf<Int>()
    /** Tabs from the last full rebuild — used to skip redundant rebuilds on resume. */
    private var lastBuiltTabs: List<ExpandedTabConfig> = emptyList()

    /** Whether to show weather as the circular cookie badge (true) or as a regular page (false). */
    private var showWeatherCookie = true

    /** All non-weather Smartspace targets available to page through in the header pill. */
    private var headerTargets: List<Item.Target> = emptyList()
    /** Index into [headerTargets] of the target currently shown in the header pill. */
    private var currentHeaderIndex = 0
    private var dotViews: List<View> = emptyList()
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

    // StateFlow already has a value — read it directly instead of blocking the main thread.
    private val isDark get() = wallpaperRepository.homescreenWallpaperDarkTextColour.value
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
        setBlurEnabled(true)
        setupLoading()
        setupState()
        setupMonet()
        setupInsets()
        setupUnlock()
        setupOverlaySwipe()
        setupDisabledButton()
        setupClose()
        setupMenuButton()
        setupDoodleHeader()
        setupOpenAppButton()
        setupTabs()
        setupHeaderSwipe()
        viewModel.setup(isOverlay)
        handleLaunchNavigation()
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    private fun setupLoading() {
        (binding.expandedLoading.drawable as? AnimatedVectorDrawable)?.start()
    }

    private fun setupMonet() {
        reapplyColors()
        // Reactively reapply when the background mode changes (e.g. switched from
        // the Settings tab while the expanded view is still visible).
        viewLifecycleOwner.whenResumed {
            settingsRepository.expandedBackground.asFlow().drop(1).collect {
                reapplyColors()
                setBlurEnabled(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lastBuiltTabs = emptyList()
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

        // Pill + menu container use colorSecondaryContainer (50% in BLUR mode)
        val secondaryContainer = blurAlpha(ctx.getAttrColor(com.google.android.material.R.attr.colorSecondaryContainer))
        val onSecondaryContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
        binding.expandedHeaderPill.setCardBackgroundColor(secondaryContainer)
        // In BLUR/SCRIM modes the pill itself provides the frosted surface; the
        // floating containers should be fully transparent so they don't add a
        // second opaque layer on top of the blur.
        val floatingContainerColor = if (isBlurBackground) Color.TRANSPARENT else secondaryContainer
        binding.expandedHeaderMenuContainer.setCardBackgroundColor(floatingContainerColor)
        binding.expandedHeaderWeatherContainer.setCardBackgroundColor(floatingContainerColor)

        val primaryContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorPrimaryContainer)
        val onPrimaryContainer = ctx.getAttrColor(com.google.android.material.R.attr.colorOnPrimaryContainer)

        // Rebuild the three menu dots (5×5dp, 2dp gap, colorOnSecondaryContainer)
        binding.expandedHeaderMenu.removeAllViews()
        val dotPx = 5.dp
        val gapPx = 2.dp
        repeat(3) { i ->
            binding.expandedHeaderMenu.addView(View(ctx).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(onSecondaryContainer)
                }
                layoutParams = LinearLayout.LayoutParams(dotPx, dotPx).apply {
                    if (i > 0) topMargin = gapPx
                }
            })
        }

        // Squircle uses colorPrimaryContainer
        binding.expandedHeaderWeather.setBackgroundColor(primaryContainer)
        binding.expandedHeaderWeatherTemp.setTextColor(onPrimaryContainer)

        // Nav pill (tab scroll container) — always fully opaque
        val surfaceContainerHigh = ctx.getAttrColor(com.google.android.material.R.attr.colorSurfaceContainerHigh)
        binding.expandedTabScrollPill.setCardBackgroundColor(surfaceContainerHigh)
        val elevationOverlay = com.google.android.material.elevation.ElevationOverlayProvider(ctx)
        val tintedPillColor = elevationOverlay.compositeOverlayIfNeeded(
            surfaceContainerHigh, binding.expandedTabScrollPill.cardElevation
        )
        binding.expandedTabScrollPill.strokeColor = tintedPillColor

        // FAB — secondary-fixed-dim background, on-secondary-fixed icon (same in both modes).
        // On API 31+ use the system Monet palette directly (always correct).
        // Below API 31 fall back to MonetCompat's shade lookup.
        val secondaryFixedDim = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ctx.getColor(android.R.color.system_accent2_200)
        } else {
            ctx.getAttrColor(com.google.android.material.R.attr.colorSecondaryFixedDim)
        }
        val onSecondaryFixed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ctx.getColor(android.R.color.system_accent2_900)
        } else {
            ctx.getAttrColor(com.google.android.material.R.attr.colorOnSecondaryFixed)
        }
        binding.expandedTabOpenApp.backgroundTintList = ColorStateList.valueOf(secondaryFixedDim)
        binding.expandedTabOpenApp.iconTint = ColorStateList.valueOf(onSecondaryFixed)

        // Doodle header menu — same 5×5dp dots, colored for the window background
        val onBackground = ctx.getAttrColor(com.google.android.material.R.attr.colorOnBackground)
        binding.expandedDoodleMenu.removeAllViews()
        repeat(3) { i ->
            binding.expandedDoodleMenu.addView(View(ctx).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(onBackground)
                }
                layoutParams = LinearLayout.LayoutParams(dotPx, dotPx).apply {
                    if (i > 0) topMargin = gapPx
                }
            })
        }

        // Empty-state label
        val onSurfaceVariant = ctx.getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        binding.expandedEmptyLabel.setTextColor(onSurfaceVariant)

        // Tab buttons — re-select current tab so selected/unselected colours refresh
        selectTab(currentTabIndex)
    }

    private fun setupInsets() {
        binding.expandedUnlockContainer.onApplyInsets { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
        }
        binding.expandedTabNavigation.onApplyInsets { view, insets ->
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = navBarBottom + 16.dp
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
        binding.expandedHeaderMenu.onClicked().collect { navigateToTabSettings() }
    }

    private fun navigateToTabSettings() {
        if (isOverlay || isMinusOne) {
            launchOverlayAction(OpenFromOverlayAction.AddWidget(0), navigateToTabSettings = true)
        } else {
            findNavController().navigate(R.id.action_expandedFragment_to_expandedTabSettingsFragment)
        }
    }

    /**
     * Observes [SmartspacerSettingsRepository.expandedShowDoodle] and toggles the doodle header
     * row above the pill.  When enabled:
     *  - The doodle header (image + ⋮ button) becomes visible.
     *  - The pill's own ⋮ menu container is hidden (INVISIBLE to preserve VP2 constraints).
     *  - The doodle image is loaded via Glide, respecting dark mode.
     */
    private fun setupDoodleHeader() {
        binding.expandedDoodleMenu.setOnClickListener { navigateToTabSettings() }
        binding.expandedDoodleImage.setOnClickListener { openDoodleTarget() }
        whenResumed {
            settingsRepository.expandedShowDoodle.asFlow().collect { enabled ->
                isDoodleEnabled = enabled
                syncDoodleHeaderVisibility()
                if (enabled) loadDoodleImage()
            }
        }
    }

    private fun openDoodleTarget() {
        val openGoogleApp = settingsRepository.expandedDoodleOpenGoogleApp.getSync()
        if (openGoogleApp) {
            val googleAppIntent = requireContext().packageManager
                .getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
            if (googleAppIntent != null) {
                startActivity(googleAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
        }
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun syncDoodleHeaderVisibility() {
        val isLoaded = viewModel.state.value is State.Loaded
        val show = isDoodleEnabled && isLoaded
        binding.expandedDoodleHeader.isVisible = show
        // GONE — VP2's end-constraint resolves to the pill's right edge (or before the weather
        // cookie if visible) so pages get the full available width when the doodle is shown.
        binding.expandedHeaderMenuContainer.isVisible = !show
    }

    private fun loadDoodleImage() = whenResumed {
        val doodle = searchRepository.getDoodle()
        val density = requireContext().resources.displayMetrics.density
        if (doodle.url == DoodleImage.DEFAULT.url) {
            // Wordmark: 36dp height, centered vertically in the 48dp header.
            binding.expandedDoodleImage.updateLayoutParams<LinearLayout.LayoutParams> {
                height = (36 * density).toInt()
            }
            binding.expandedDoodleImage.scaleType = ImageView.ScaleType.FIT_START
            val secondary = requireContext().getAttrColor(com.google.android.material.R.attr.colorSecondary)
            binding.expandedDoodleImage.setImageResource(R.drawable.ic_google_logo_monet)
            binding.expandedDoodleImage.imageTintList = ColorStateList.valueOf(secondary)
        } else {
            // Real doodle: fill the full 48dp header height.
            binding.expandedDoodleImage.updateLayoutParams<LinearLayout.LayoutParams> {
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }
            binding.expandedDoodleImage.scaleType = ImageView.ScaleType.FIT_START
            binding.expandedDoodleImage.imageTintList = null
            val url = if (requireContext().isDarkMode) doodle.darkUrl ?: doodle.url else doodle.url
            try {
                Glide.with(this@ExpandedFragment).asBitmap().load(url).into(binding.expandedDoodleImage)
            } catch (_: Exception) { /* doodle unavailable */ }
        }
    }

    /** If the activity was launched with [EXTRA_NAVIGATE_TAB_SETTINGS], navigate once then clear. */
    private fun handleLaunchNavigation() {
        val intent = requireActivity().intent ?: return
        if (!intent.getBooleanExtra(EXTRA_NAVIGATE_TAB_SETTINGS, false)) return
        intent.removeExtra(EXTRA_NAVIGATE_TAB_SETTINGS)
        binding.root.post {
            if (isAdded && !isDetached) {
                findNavController().navigate(R.id.action_expandedFragment_to_expandedTabSettingsFragment)
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
        // The nav container uses constraintWidth_default="wrap" with 36dp margins, so
        // ConstraintLayout naturally centers it when content is narrow and limits it to
        // screen-72dp (36dp each side) when tabs overflow — no dynamic margin tracking needed.
        whenResumed {
            tabRepository.tabs.collect { tabs ->
                // Only update visibility when loaded — handleState owns visibility in other states.
                if (viewModel.state.value is State.Loaded) buildTabUI(tabs)
            }
        }
        // Re-apply button icons/text whenever the display mode changes, even if the tab list
        // itself hasn't changed (StateFlow wouldn't re-emit an identical tab list).
        whenResumed {
            tabRepository.navItemDisplayMode.drop(1).collect { selectTab(currentTabIndex) }
        }
    }

    private fun buildTabUI(tabs: List<ExpandedTabConfig>) {
        // Always update visibility so that returning from a Disabled/Loading state
        // correctly restores the content area even when the tab list hasn't changed.
        if (tabs.isEmpty()) {
            binding.expandedWidgetFlipper.isVisible = false
            binding.expandedEmptyLabel.isVisible = true
            binding.expandedTabScrollPill.isVisible = false
        } else {
            binding.expandedWidgetFlipper.isVisible = true
            binding.expandedEmptyLabel.isVisible = false
            binding.expandedTabScrollPill.isVisible = true
        }

        // If the tab list hasn't changed, don't tear down and rebuild the views.
        // This preserves scroll positions and the selected tab when the fragment
        // resumes (StateFlow replays its current value on each new collection).
        if (tabs == lastBuiltTabs) {
            // Still need to re-apply button visuals in case display mode changed.
            selectTab(currentTabIndex)
            return
        }
        lastBuiltTabs = tabs

        if (tabs.isEmpty()) {
            loadedTabIndices.clear()
            currentTabIndex = 0
            return
        }

        // Clamp the preserved index to the new tab count; reset to 0 only when necessary.
        val preservedIndex = currentTabIndex.coerceIn(0, tabs.size - 1)
        loadedTabIndices.clear()
        currentTabIndex = preservedIndex

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
        // Remove trailing gap from last item so the pill wraps content tightly.
        (binding.expandedTabButtons.getChildAt(tabs.size - 1)?.layoutParams
                as? LinearLayout.LayoutParams)?.marginEnd = 0
        selectTab(preservedIndex)
    }

    private fun setupHeaderSwipe() {
        // ViewPager2 handles finger-tracking and page snapping directly.
        // The pill's SwipeDetectingCardView still fires requestDisallowInterceptTouchEvent on
        // ACTION_DOWN to guard against SlidingPanelLayout in the overlay context, but its own
        // fling-intercept is disabled (onHorizontalSwipe = null) so it never cancels the pager.
        binding.expandedHeaderPill.onHorizontalSwipe = null

        // ViewPager2 is constrained in XML to end at the left edge of the weather cookie,
        // so each page fills the text area exactly and clips cleanly at the cookie boundary.
        // No right padding is needed; the cookie itself acts as the natural occlusion point
        // — the incoming page slides in from behind the cookie during a swipe.
        binding.expandedHeaderTarget.apply {
            clipToPadding = true
            offscreenPageLimit = 1
        }

        // Composite transformer:
        //  1. Dynamic gap applied only to pages with position > 0 (incoming from the right).
        //     • At rest (position = 1): translationX = gap → adjacent page pushed exactly to the
        //       pill's right edge, hidden behind the weather cookie / menu container.
        //     • Mid-swipe: translationX = gap * position → page slides out from behind the
        //       cookie proportionally as the user drags.
        //     • Completion (position = 0): translationX = 0 with no jump (gap * 0 = 0).
        //     • Current / outgoing pages (position ≤ 0): translationX = 0 → move 1:1 with
        //       finger, no acceleration artifacts.
        //     Gap re-read at transform time so it auto-adjusts when the weather cookie or
        //     doodle header appear / disappear.
        //  2. Scale + fade pages as they move off-centre (subtle depth)
        val transformer = CompositePageTransformer()
        transformer.addTransformer { page, position ->
            val gap = (binding.expandedHeaderPill.width - binding.expandedHeaderTarget.width).toFloat()
            page.translationX = if (position > 0f) gap * position else 0f
        }
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
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    updateDotWidths(position, positionOffset)
                }
            }
        )
    }

    private fun selectTab(index: Int) {
        val tabs = tabRepository.getTabs()
        if (index < 0 || index >= tabs.size) return
        val previousIndex = currentTabIndex
        currentTabIndex = index
        binding.expandedWidgetFlipper.displayedChild = index

        // Reset scroll to top when the user actively switches to a different tab.
        if (index != previousIndex) {
            val container = binding.expandedWidgetFlipper.getChildAt(index) as? FrameLayout
            (container?.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)
                ?.scrollToPosition(0)
        }

        val ctx = requireContext()
        // M3 nav-bar tokens
        val primaryColor = ctx.getAttrColor(androidx.appcompat.R.attr.colorPrimary)
        val onPrimary = ctx.getAttrColor(com.google.android.material.R.attr.colorOnPrimary)
        val onSurfaceVariant = ctx.getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        val displayMode = tabRepository.getNavItemDisplayMode()
        val symbolTypeface = MaterialSymbolsHelper.getTypeface(ctx)

        // Active indicator pill — 40 dp tall, 20 dp corner radius (Figma node 29:730).
        fun pillBg(color: Int): GradientDrawable = GradientDrawable().apply {
            cornerRadius = 24.dp.toFloat()
            setColor(color)
        }

        // If the previously-selected tab is to the LEFT of the newly-selected tab, its icon
        // will collapse and shift the selected tab left by (iconWidth + marginEnd).  Capture
        // this now so the scroll target can account for it when both animations run in parallel.
        // Only ICON_AND_LABEL mode collapses the icon; ICON_ONLY items are fixed-width circles
        // with no collapse animation, so collapseShift must stay 0 there.
        var collapseShift = 0
        if (displayMode == NavItemDisplayMode.ICON_AND_LABEL && previousIndex in 0 until index) {
            val prevItemView = binding.expandedTabButtons.getChildAt(previousIndex)
            val prevIconContainer = prevItemView?.findViewById<FrameLayout>(R.id.nav_item_icon_container)
            if (prevIconContainer?.visibility == View.VISIBLE) {
                val w = prevIconContainer.width
                val m = (prevIconContainer.layoutParams as? LinearLayout.LayoutParams)?.marginEnd ?: 0
                if (w > 0) collapseShift = w + m
            }
        }

        for (i in 0 until binding.expandedTabButtons.childCount) {
            val itemView = binding.expandedTabButtons.getChildAt(i) ?: continue
            val iconContainer = itemView.findViewById<FrameLayout>(R.id.nav_item_icon_container) ?: continue
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

            when (displayMode) {
                NavItemDisplayMode.LABEL_ONLY -> {
                    if (itemView.layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                        itemView.layoutParams = itemView.layoutParams.also { it.width = ViewGroup.LayoutParams.WRAP_CONTENT }
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                        (itemView as? LinearLayout)?.gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    iconContainer.visibility = View.GONE
                    label.visibility = View.VISIBLE
                    label.text = tab.label
                    if (isSelected) {
                        label.setTextColor(onPrimary)
                        itemView.background = pillBg(primaryColor)
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                    } else {
                        label.setTextColor(onSurfaceVariant)
                        itemView.background = null
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                    }
                }

                NavItemDisplayMode.ICON_ONLY -> {
                    val circleSize = itemView.height.takeIf { it > 0 } ?: 47.dp
                    if (itemView.layoutParams.width != circleSize) {
                        itemView.layoutParams = itemView.layoutParams.also { it.width = circleSize }
                        itemView.setPadding(0, 0, 0, 0)
                    }
                    (itemView as? LinearLayout)?.gravity = android.view.Gravity.CENTER
                    (iconContainer.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = 0
                    label.visibility = View.GONE
                    val cp = tab.iconCodepoint
                    if (cp != null) {
                        setIcon(cp, if (isSelected) onPrimary else onSurfaceVariant)
                        iconContainer.visibility = View.VISIBLE
                    } else {
                        iconContainer.visibility = View.GONE
                    }
                    val circleRadius = 100.dp.toFloat()
                    if (isSelected) {
                        itemView.background = GradientDrawable().apply { cornerRadius = circleRadius; setColor(primaryColor) }
                        itemView.setPadding(0, 0, 0, 0)
                        itemView.layoutParams = itemView.layoutParams.also { it.width = circleSize }
                    } else {
                        itemView.background = null
                        itemView.setPadding(0, 0, 0, 0)
                        itemView.layoutParams = itemView.layoutParams.also { it.width = circleSize }
                    }
                }

                NavItemDisplayMode.ICON_AND_LABEL -> {
                    if (itemView.layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                        itemView.layoutParams = itemView.layoutParams.also { it.width = ViewGroup.LayoutParams.WRAP_CONTENT }
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                        (itemView as? LinearLayout)?.gravity = android.view.Gravity.CENTER_VERTICAL
                        (iconContainer.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = 4.dp
                    }
                    label.visibility = View.VISIBLE
                    label.text = tab.label
                    val cp = tab.iconCodepoint
                    if (isSelected) {
                        itemView.background = pillBg(primaryColor)
                        label.setTextColor(onPrimary)
                        if (cp != null) {
                            setIcon(cp, onPrimary)
                            iconContainer.visibility = View.VISIBLE
                            // Measure icon at natural size (UNSPECIFIED so parent doesn't constrain it).
                            iconView.measure(
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                            )
                            val naturalWidth = iconView.measuredWidth
                            if (index != previousIndex && naturalWidth > 0) {
                                // Fix icon at natural px width so it overflows the container
                                // and clipChildren can crop it as the container grows from 0.
                                (iconView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                                    lp.width = naturalWidth
                                    iconView.layoutParams = lp
                                }
                                // Start with both width=0 and marginEnd=0 so the container
                                // contributes zero total space — no layout jump on GONE→VISIBLE.
                                (iconContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                                    lp.width = 0
                                    lp.marginEnd = 0
                                    iconContainer.layoutParams = lp
                                }
                                ValueAnimator.ofFloat(0f, 1f).apply {
                                    duration = 150L
                                    addUpdateListener {
                                        val frac = animatedValue as Float
                                        (iconContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                                            lp.width = (naturalWidth * frac).toInt()
                                            lp.marginEnd = (4.dp * frac).toInt()
                                            iconContainer.layoutParams = lp
                                        }
                                    }
                                    addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(a: android.animation.Animator) {
                                            (iconView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                                                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                                                iconView.layoutParams = lp
                                            }
                                            (iconContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                                                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                                                lp.marginEnd = 4.dp
                                                iconContainer.layoutParams = lp
                                            }
                                        }
                                    })
                                    start()
                                }
                            }
                        } else {
                            iconContainer.visibility = View.GONE
                        }
                    } else if (wasSelected) {
                        // Deselect: change style immediately, shrink icon container to 0.
                        itemView.background = null
                        label.setTextColor(onSurfaceVariant)
                        iconView.setTextColor(onSurfaceVariant)
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                        val startWidth = iconContainer.width
                        if (startWidth > 0 && iconContainer.visibility == View.VISIBLE) {
                            // Fix icon at current px width so it overflows the shrinking
                            // container and clipChildren crops it as the container collapses.
                            val iconCurrentWidth = iconView.width.takeIf { it > 0 } ?: startWidth
                            (iconView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                                lp.width = iconCurrentWidth
                                iconView.layoutParams = lp
                            }
                            ValueAnimator.ofFloat(1f, 0f).apply {
                                duration = 150L
                                addUpdateListener {
                                    val frac = animatedValue as Float
                                    (iconContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                                        lp.width = (startWidth * frac).toInt()
                                        lp.marginEnd = (4.dp * frac).toInt()
                                        iconContainer.layoutParams = lp
                                    }
                                }
                                addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(a: android.animation.Animator) {
                                        iconContainer.visibility = View.GONE
                                        (iconView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                                            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                                            iconView.layoutParams = lp
                                        }
                                        (iconContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                                            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                                            lp.marginEnd = 4.dp
                                            iconContainer.layoutParams = lp
                                        }
                                    }
                                })
                                start()
                            }
                        } else {
                            iconContainer.visibility = View.GONE
                        }
                    } else {
                        itemView.background = null
                        label.setTextColor(onSurfaceVariant)
                        iconContainer.visibility = View.GONE
                        itemView.setPadding(8.dp, 0, 8.dp, 0)
                    }
                }
            }
        }

        if (!loadedTabIndices.contains(index)) {
            loadedTabIndices.add(index)
            loadReadYouArticlesForTab(index, tabs[index].appWidgetId)
        }

        // Start scroll immediately (parallel with icon animations).  Subtract collapseShift so
        // the target is the FINAL position of the selected tab after the prev tab's icon shrinks.
        binding.expandedTabScrollView.post {
            val sv = binding.expandedTabScrollView
            val item = binding.expandedTabButtons.getChildAt(index) ?: return@post
            val targetX = (item.left - collapseShift).coerceAtLeast(0)
            val curX = sv.scrollX
            if (targetX == curX) return@post
            ValueAnimator.ofInt(curX, targetX).apply {
                duration = 250L
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { sv.scrollTo(animatedValue as Int, 0) }
                start()
            }
        }
    }

    private fun loadReadYouArticlesForTab(tabIndex: Int, appWidgetId: Int) {
        val container = binding.expandedWidgetFlipper.getChildAt(tabIndex) as? FrameLayout ?: return

        val cardInset = 16.dp
        val topRadius = 28.dp.toFloat()

        val recycler = androidx.recyclerview.widget.RecyclerView(requireContext()).apply {
            visibility = android.view.View.INVISIBLE
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            addItemDecoration(ReadYouArticleAdapter.ItemDecoration(5.dp))
            clipToPadding = false
            // Clip to match the card edges (16dp inset each side) with 28dp top corners,
            // mirroring the Figma overflow-clip container. Bottom extends off-screen so
            // only the top corners are rounded.
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: Outline) {
                    if (view.width == 0 || view.height == 0) return
                    outline.setRoundRect(
                        cardInset, 0,
                        view.width - cardInset, view.height + topRadius.toInt(),
                        topRadius
                    )
                }
            }
            clipToOutline = true
            doOnAttach { invalidateOutline() }
            // Bottom padding so the last card stops 9dp above the top of the nav pill.
            // Computed from the nav view's actual laid-out position.
            doOnAttach { rv ->
                val setBottomPadding = {
                    val navTop = binding.expandedTabNavigation.top
                    if (navTop > 0) {
                        rv.updatePadding(bottom = binding.root.height - navTop + 16.dp)
                    }
                }
                setBottomPadding()
                val navListener = android.view.View.OnLayoutChangeListener { _, _, top, _, _, _, _, _, _ ->
                    if (top > 0) rv.updatePadding(bottom = binding.root.height - top + 16.dp)
                }
                binding.expandedTabNavigation.addOnLayoutChangeListener(navListener)
                rv.doOnDetach {
                    binding.expandedTabNavigation.removeOnLayoutChangeListener(navListener)
                }
            }
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                    // Allow parent to intercept when RecyclerView is at the top and user scrolls up
                    recyclerView.parent?.requestDisallowInterceptTouchEvent(
                        newState != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE ||
                        recyclerView.canScrollVertically(-1)
                    )
                }
            })
        }
        val indicatorHf         = 48.dp.toFloat()   // determinate indicator size
        val indicatorHfSyncing  = 56.dp.toFloat()   // indeterminate indicator size

        var pullFraction by androidx.compose.runtime.mutableStateOf(0f)
        var isSyncing    by androidx.compose.runtime.mutableStateOf(false)

        val indicatorColor = androidx.compose.ui.graphics.Color(
            requireContext().getAttrColor(androidx.appcompat.R.attr.colorPrimary)
        )
        val loadingIndicator = androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            scaleX = 0f
            scaleY = 0f
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            // No explicit Modifier.size() — both branches use Material3's natural default so the
            // ComposeView never changes size when switching between determinate and indeterminate.
            setContent {
                if (isSyncing) {
                    androidx.compose.material3.LoadingIndicator(
                        modifier = androidx.compose.ui.Modifier.size(androidx.compose.ui.unit.Dp(56f)),
                        color = indicatorColor,
                    )
                } else {
                    androidx.compose.material3.LoadingIndicator(
                        progress = { pullFraction },
                        modifier = androidx.compose.ui.Modifier.size(androidx.compose.ui.unit.Dp(48f)),
                        color = indicatorColor,
                    )
                }
            }
        }

        container.removeAllViews()
        container.clipChildren = false
        binding.expandedWidgetFlipper.clipChildren = false
        container.addView(recycler, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        container.addView(loadingIndicator, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        })

        val maxContentShift = 60.dp.toFloat()
        // The widget flipper sits 16dp below the smartspace pill (layout_marginTop="16dp").
        // To centre the indicator in the full visual space (pill bottom → first card top) we
        // shift up by half that margin so the midpoint lands between the two edges.
        val pillMarginOffset = 8.dp.toFloat()   // 16dp / 2
        fun indicatorY(gap: Float, hf: Float = indicatorHf) = gap / 2f - hf / 2f - pillMarginOffset

        fun showLoadingIndicator() {
            isSyncing = true   // switch to full indeterminate expressive animation
            loadingIndicator.rotation = 0f
            val targetGap = maxContentShift
            loadingIndicator.translationY = indicatorY(targetGap, indicatorHfSyncing)
            loadingIndicator.animate().cancel()
            loadingIndicator.animate().scaleX(1f).scaleY(1f).setDuration(150L)
                .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            recycler.animate().cancel()
            recycler.animate().translationY(targetGap).setDuration(150L)
                .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
        }

        fun hideLoadingIndicator(onHidden: () -> Unit) {
            isSyncing = false
            pullFraction = 0f
            loadingIndicator.rotation = 0f
            loadingIndicator.animate().cancel()
            recycler.animate().cancel()
            val startGap   = recycler.translationY
            val startScale = loadingIndicator.scaleX
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 300L
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener { anim ->
                    val f = anim.animatedValue as Float
                    val gap = startGap * f
                    recycler.translationY = gap
                    loadingIndicator.translationY = indicatorY(gap)
                    loadingIndicator.scaleX = startScale * f
                    loadingIndicator.scaleY = startScale * f
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        loadingIndicator.scaleX = 0f
                        loadingIndicator.scaleY = 0f
                        onHidden()
                    }
                })
                start()
            }
        }

        // Loads articles and sets/updates the adapter. Called on first load, on
        // ContentObserver notifications (background sync), and on pull-to-refresh.
        // [clearReadState] = true wipes dimmed-read marks; only set on user-triggered refresh.
        fun loadArticles(isPullToRefresh: Boolean = false) {
            viewLifecycleOwner.lifecycleScope.launch {
                // Keep the loading animation visible for at least 1000 ms on pull-to-refresh.
                val minDisplayJob = if (isPullToRefresh) {
                    launch { kotlinx.coroutines.delay(1000) }
                } else null
                val articles = readYouRepository.getArticles(appWidgetId)
                articles.filter { it.isRead }.forEach { readYouRepository.markArticleRead(it.id) }
                minDisplayJob?.join()
                val existing = recycler.adapter as? ReadYouArticleAdapter
                if (existing == null) {
                    val cardColor = blurAlpha(requireContext()
                        .getAttrColor(com.google.android.material.R.attr.colorSurfaceContainer))
                    recycler.adapter = ReadYouArticleAdapter(
                        articles,
                        isRead = { readYouRepository.isArticleRead(it) },
                        onMarkRead = { readYouRepository.markArticleRead(it) },
                        cardBackgroundColor = cardColor
                    ) { article ->
                        val (filterFeedId, filterGroupId) = readYouRepository.getFilterIds(appWidgetId)
                        launchReadYouArticle(article.id, filterFeedId, filterGroupId)
                    }
                    recycler.visibility = android.view.View.VISIBLE
                } else {
                    existing.updateArticles(articles, clearRead = isPullToRefresh)
                }
                hideLoadingIndicator {}
            }
        }

        // Pull-to-refresh touch logic:
        //
        //  Normal zone (rawDy 0 → thresholdRawDy):
        //    gap   = frac * maxContentShift     linearly 0 → 60 dp
        //    scale = frac                       indicator grows 0 → 1
        //
        //  Elastic zone (rawDy > thresholdRawDy):
        //    gap   = maxContentShift + extra * elasticFactor  (slow drift, ~12% finger speed)
        //    scale stays at 1
        //
        //  On release past threshold → animation keeps playing + refresh fires.
        //
        //  thresholdRawDy = indicatorHf / pullResistance  (finger travel to full gap)
        //
        //  Animation phases (pullFraction / rotation):
        //   Phase 1 (0 → 0.75×threshold):             frame 0, no rotation
        //   Phase 2 (0.75× → 1.75×threshold):         determinate 0→1 over 1.0×threshold distance
        //   Phase 3 (past 1.75×threshold):             final frame, rotates; 360° per 5×threshold px
        val pullResistance = 0.4f   // finger travel → effective pull (< 1 = slower gap growth)
        val elasticFactor = 0.12f  // feed drifts at 12% of finger speed past threshold
        recycler.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var pullStartY  = 0f
            private var isPulling   = false
            private var hasTriggered = false

            // rawDy at which gap saturates at maxContentShift and elastic begins.
            // pull = rawDy * pullResistance; saturates when pull = indicatorHf.
            private val thresholdRawDy = indicatorHf / pullResistance
            private val phase1End = thresholdRawDy * 0.75f
            private val phase2End = thresholdRawDy * 1.75f

            private fun updateForPull(rawDy: Float) {
                val clamped = rawDy.coerceAtLeast(0f)

                // Normal zone: feed moves linearly 0 → maxContentShift
                val frac = (clamped / thresholdRawDy).coerceIn(0f, 1f)
                val normalGap = frac * maxContentShift

                // Elastic zone: feed drifts further at elasticFactor speed
                val elasticExtra = if (clamped > thresholdRawDy) {
                    (clamped - thresholdRawDy) * elasticFactor
                } else 0f

                val totalGap = normalGap + elasticExtra
                recycler.translationY = totalGap
                loadingIndicator.translationY = indicatorY(totalGap)

                // Scale 0→1 tracks normal-zone pull; stays 1 in elastic zone
                loadingIndicator.scaleX = frac
                loadingIndicator.scaleY = frac

                // Three-phase animation:
                // Phase 1: hold on first frame (0 → 0.75×threshold)
                // Phase 2: determinate 0→1 over 1.0×threshold distance
                //          (0.75× → 1.75×threshold)
                // Phase 3: final frame + rotation (360° per 5×threshold of extra drag)
                when {
                    clamped <= phase1End -> {
                        pullFraction = 0f
                        loadingIndicator.rotation = 0f
                    }
                    clamped <= phase2End -> {
                        pullFraction = (clamped - phase1End) / (phase2End - phase1End)
                        loadingIndicator.rotation = 0f
                    }
                    else -> {
                        pullFraction = 1f
                        val extraPull = clamped - phase2End
                        loadingIndicator.rotation = -((extraPull / (thresholdRawDy * 5f)) * 360f)
                    }
                }
            }

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pullStartY = e.rawY
                        isPulling = false
                        hasTriggered = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (hasTriggered) return false
                        val rawDy = e.rawY - pullStartY
                        if (!isPulling && rawDy > 8.dp
                            && !rv.canScrollVertically(-1)
                            && rv.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                            isPulling = true
                        }
                        if (isPulling) {
                            updateForPull(rawDy)
                            return true
                        }
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                when (e.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        if (!hasTriggered) updateForPull(e.rawY - pullStartY)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isPulling && !hasTriggered) {
                            // Trigger when user released after fully pulling past threshold
                            if ((e.rawY - pullStartY) >= thresholdRawDy) {
                                hasTriggered = true
                                showLoadingIndicator()
                                loadArticles(isPullToRefresh = true)
                                // Also refresh every other feed so read articles
                                // are cleared and content is up-to-date across all tabs.
                                refreshAllTabArticles(skipIndex = tabIndex)
                            } else {
                                hideLoadingIndicator {}
                            }
                        }
                        isPulling = false
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        loadArticles()

        // Watch for new articles from Read You and refresh without resetting scroll.
        // Register on the root authority URI with notifyForDescendants=true so we catch
        // notifications regardless of whether Read You notifies on the root or per-widget URI.
        val rootContentUri = android.net.Uri.parse(
            "content://${ReadYouRepository.AUTHORITY}"
        )
        val observer = object : android.database.ContentObserver(
            android.os.Handler(android.os.Looper.getMainLooper())
        ) {
            override fun onChange(selfChange: Boolean) = loadArticles()
        }
        recycler.doOnAttach {
            requireContext().contentResolver.registerContentObserver(
                rootContentUri, true, observer
            )
            recycler.doOnDetach {
                requireContext().contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    /** Refreshes articles for the currently visible tab without resetting scroll position. */
    private fun refreshCurrentTabArticles() {
        if (lastBuiltTabs.isEmpty()) return
        val tabIndex = currentTabIndex.coerceIn(0, lastBuiltTabs.lastIndex)
        val container = binding.expandedWidgetFlipper.getChildAt(tabIndex) as? FrameLayout ?: return
        val recycler = container.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return
        val adapter = recycler.adapter as? ReadYouArticleAdapter ?: return
        val appWidgetId = lastBuiltTabs[tabIndex].appWidgetId
        viewLifecycleOwner.lifecycleScope.launch {
            val articles = readYouRepository.getArticles(appWidgetId)
            adapter.updateArticles(articles)
        }
    }

    /**
     * Refreshes every ReadYou tab's article list, removing read articles.
     * [skipIndex] is excluded (used to skip the tab already being refreshed by pull-to-refresh).
     */
    private fun refreshAllTabArticles(skipIndex: Int = -1) {
        if (lastBuiltTabs.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            lastBuiltTabs.forEachIndexed { index, tab ->
                if (index == skipIndex) return@forEachIndexed
                val container = binding.expandedWidgetFlipper.getChildAt(index) as? FrameLayout
                    ?: return@forEachIndexed
                val recycler = container.getChildAt(0)
                    as? androidx.recyclerview.widget.RecyclerView ?: return@forEachIndexed
                val adapter = recycler.adapter as? ReadYouArticleAdapter ?: return@forEachIndexed
                val articles = readYouRepository.getArticles(tab.appWidgetId)
                adapter.updateArticles(articles)
            }
        }
    }

    private fun launchReadYouArticle(articleId: String, filterFeedId: String?, filterGroupId: String?) {
        val intent = Intent().apply {
            setClassName("me.ash.reader", "me.ash.reader.infrastructure.android.MainActivity")
            putExtra("article.id", articleId)
            if (filterFeedId != null)  putExtra("feed.id", filterFeedId)
            if (filterGroupId != null) putExtra("group.id", filterGroupId)
        }
        try { startActivity(intent) } catch (_: Exception) { }
    }

    // ── State ─────────────────────────────────────────────────────────────

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed { viewModel.state.collect { handleState(it) } }
        showWeatherCookie = settingsRepository.expandedShowWeatherCookie.getSync()
        whenResumed {
            settingsRepository.expandedShowWeatherCookie.asFlow().collect { show ->
                showWeatherCookie = show
                // Re-render with current cached data so the change is instant
                updateHeaderTargets(viewModel.rawPageTargets.value)
                updateWeatherCookie(smartspaceRepository.getDefaultHomeActions().value)
            }
        }
        whenResumed { viewModel.rawPageTargets.collect { updateHeaderTargets(it) } }
        updateWeatherCookie(smartspaceRepository.getDefaultHomeActions().value)
        whenResumed { smartspaceRepository.getDefaultHomeActions().collect { updateWeatherCookie(it) } }
    }

    private fun handleState(state: State) {
        val isLoaded = state is State.Loaded
        binding.expandedLoading.isVisible = state is State.Loading
        binding.expandedHeaderPill.isVisible = isLoaded
        binding.expandedTabNavigation.isVisible = isLoaded
        binding.expandedDisabled.isVisible = state is State.Disabled
        binding.expandedPermission.isVisible = state is State.PermissionRequired
        if (!isLoaded) {
            // Hide the news feed content so it doesn't show through the disabled/loading overlay.
            binding.expandedWidgetFlipper.isVisible = false
            binding.expandedEmptyLabel.isVisible = false
            binding.expandedTabScrollPill.isVisible = false
        }
        syncDoodleHeaderVisibility()
        if (state is State.Loaded) {
            binding.expandedUnlockContainer.isVisible = state.isLocked && !isOverlay && !isMinusOne
            setStatusBarLight(state.lightStatusIcons)
            updateHeaderTarget(state.items)
            // Restore content visibility and retry widget load when returning from a non-Loaded state.
            val tabs = tabRepository.getTabs()
            buildTabUI(tabs)
            if (tabs.isNotEmpty()) {
                val container = binding.expandedWidgetFlipper.getChildAt(currentTabIndex) as? FrameLayout
                if (container != null && container.childCount == 0) {
                    loadedTabIndices.remove(currentTabIndex)
                    loadReadYouArticlesForTab(currentTabIndex, tabs[currentTabIndex].appWidgetId)
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
     * Rebuilds [headerTargets] from the raw session page list.
     * When [showWeatherCookie] is true (default), weather targets are stripped from the pager
     * and shown as the circular cookie badge instead. When false, weather targets are kept in
     * the pager as regular pages and the cookie is hidden.
     */
    private fun updateHeaderTargets(rawTargets: List<SmartspaceTarget>) {
        val targets = if (showWeatherCookie) {
            rawTargets
                .filter { it.featureType != FEATURE_WEATHER }
                .filterNot { t ->
                    // Belt-and-suspenders: also drop blank complication targets whose action
                    // carries subcardType=FEATURE_WEATHER (featureType on these is UNDEFINED).
                    t.smartspaceTargetId.startsWith(BLANK_TARGET_PREFIX) &&
                    (t.headerAction?.extras?.getInt("subcardType", -1) == FEATURE_WEATHER ||
                     t.baseAction?.extras?.getInt("subcardType", -1) == FEATURE_WEATHER)
                }
        } else {
            rawTargets
        }.map { target -> Item.Target(target, null, false, applyShadow = false, isDark = isDark) }
        headerTargets = targets
        if (currentHeaderIndex >= targets.size) currentHeaderIndex = 0
        // Keep all pages alive so SmartspacerView.onAttachedToWindow never fires mid-swipe
        binding.expandedHeaderTarget.offscreenPageLimit = targets.size.coerceAtLeast(1)
        headerPagerAdapter.submitTargets(targets)
        binding.expandedHeaderTarget.setCurrentItem(currentHeaderIndex, false)
        updateHeaderDots()
    }

    /**
     * Rebuilds the dot-indicator row when the page count changes.
     * Each dot is a capsule (RECTANGLE + cornerRadius=100dp) so its width can smoothly
     * interpolate between [R.dimen.header_dot_size] (inactive circle) and
     * [R.dimen.header_dot_active_width] (active pill) via [updateDotWidths].
     */
    private fun updateHeaderDots() {
        val dots = binding.expandedHeaderDots
        val count = headerTargets.size
        dots.isVisible = count > 1
        if (!dots.isVisible) {
            dots.removeAllViews()
            dotViews = emptyList()
            return
        }
        // Only rebuild views if the count changed; otherwise just refresh widths.
        if (dotViews.size != count) {
            dots.removeAllViews()
            val sizePx = resources.getDimensionPixelSize(R.dimen.header_dot_size)
            val activePx = resources.getDimensionPixelSize(R.dimen.header_dot_active_width)
            val gapPx = resources.getDimensionPixelSize(R.dimen.header_dot_gap)
            val activeColor = requireContext()
                .getAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
            val inactiveColor = androidx.core.graphics.ColorUtils.setAlphaComponent(activeColor, 128)
            dotViews = List(count) { i ->
                View(requireContext()).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 100.dp.toFloat()
                        setColor(if (i == currentHeaderIndex) activeColor else inactiveColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        if (i == currentHeaderIndex) activePx else sizePx,
                        sizePx
                    ).apply {
                        if (i > 0) marginStart = gapPx
                    }
                }.also { dots.addView(it) }
            }
        }
        // Snap to fully-settled state (no scroll in progress).
        updateDotWidths(currentHeaderIndex, 0f)
    }

    /**
     * Called from [ViewPager2.OnPageChangeCallback.onPageScrolled] to animate the active-dot
     * pill between pages as the user swipes.
     *
     * [position] is the left-hand page index; [offset] (0..1) is how far we've scrolled toward
     * [position]+1.  The active pill width interpolates from full→short on [position] and
     * short→full on [position]+1; all other dots stay at the inactive circle width.
     */
    private fun updateDotWidths(position: Int, offset: Float) {
        if (dotViews.isEmpty()) return
        val sizePx = resources.getDimensionPixelSize(R.dimen.header_dot_size)
        val activePx = resources.getDimensionPixelSize(R.dimen.header_dot_active_width)
        val activeColor = requireContext()
            .getAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
        val inactiveColor = androidx.core.graphics.ColorUtils.setAlphaComponent(activeColor, 128)
        dotViews.forEachIndexed { i, dot ->
            val targetWidth = when (i) {
                position -> (activePx + (sizePx - activePx) * offset).toInt()
                position + 1 -> (sizePx + (activePx - sizePx) * offset).toInt()
                else -> sizePx
            }
            val lp = dot.layoutParams as? LinearLayout.LayoutParams ?: return@forEachIndexed
            if (lp.width != targetWidth) {
                lp.width = targetWidth
                dot.layoutParams = lp
            }
            // Crossfade colour along with size
            val targetColor = when (i) {
                position -> if (offset < 0.5f) activeColor else inactiveColor
                position + 1 -> if (offset >= 0.5f) activeColor else inactiveColor
                else -> inactiveColor
            }
            (dot.background as? GradientDrawable)?.setColor(targetColor)
        }
    }

    private fun updateWeatherCookie(actions: List<com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction>) {
        // When cookie is disabled, always hide the badge — weather is shown as a page instead.
        if (!showWeatherCookie) {
            binding.expandedHeaderWeatherContainer.isVisible = false
            return
        }
        // getDefaultHomeActions() contains only FEATURE_WEATHER-derived actions (applyToActions()
        // filters exclusively to FEATURE_WEATHER targets). Take the first one with an icon.
        val weatherAction = actions.firstOrNull { it.icon != null }

        val icon = weatherAction?.icon
        if (icon == null) {
            binding.expandedHeaderWeatherContainer.isVisible = false
            return
        }

        try {
            val drawable = icon.loadDrawable(requireContext())
            // Adaptive icons include a solid background layer that would show as a square.
            // Extract only the foreground so the cookie background colour shows through.
            val display = if (drawable is android.graphics.drawable.AdaptiveIconDrawable) {
                drawable.foreground
            } else {
                drawable
            }
            binding.expandedHeaderWeatherIcon.setImageDrawable(display)
            binding.expandedHeaderWeatherContainer.isVisible = true
        } catch (e: Exception) {
            binding.expandedHeaderWeatherContainer.isVisible = false
            return
        }

        val temp = weatherAction.subtitle?.toString()?.takeIf { it.isNotBlank() }
            ?: weatherAction.title.takeIf { it.isNotBlank() }
        binding.expandedHeaderWeatherTemp.isVisible = !temp.isNullOrBlank()
        binding.expandedHeaderWeatherTemp.text = temp ?: ""

        // Make the weather cookie tappable — fire the action's intent or pendingIntent.
        val weatherIntent = weatherAction.intent
        val weatherPendingIntent = weatherAction.pendingIntent
        binding.expandedHeaderWeatherContainer.setOnClickListener {
            when {
                weatherIntent != null -> runCatching {
                    requireContext().startActivity(
                        weatherIntent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    )
                }
                weatherPendingIntent != null && !weatherAction.skipPendingIntent ->
                    weatherPendingIntent.sendSafely()
            }
        }
    }

    private fun setStatusBarLight(enabled: Boolean) {
        WindowCompat.getInsetsController(requireActivity().window, requireView())
            .isAppearanceLightStatusBars = enabled
    }

    // ── Blur / background ─────────────────────────────────────────────────

    /** True when the user has chosen BLUR or SCRIM — both show semi-transparent surfaces. */
    private val isBlurBackground: Boolean
        get() = settingsRepository.expandedBackground.getSync().let {
            it == ExpandedBackground.BLUR || it == ExpandedBackground.SCRIM
        }

    /** True only for BLUR — SCRIM shows the same surfaces but without window blur. */
    private val isActualBlur: Boolean
        get() = settingsRepository.expandedBackground.getSync() == ExpandedBackground.BLUR

    /**
     * Returns [color] with 75% alpha in BLUR/SCRIM mode, fully opaque otherwise.
     * Applied to every tinted surface so the wallpaper shows through.
     */
    private fun blurAlpha(color: Int): Int =
        if (isBlurBackground) androidx.core.graphics.ColorUtils.setAlphaComponent(color, 192)
        else color

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        if (!isMinusOne) {
            reapplyColors()
            setBlurEnabled(true)
        }
        // Force the alarm complication to re-read from AlarmManager so it never shows stale data.
        SmartspacerComplicationProvider.notifyChange(requireContext(), AlarmComplication::class.java)
    }

    override fun onPause() {
        // Only clear blur/window state in BLUR/SCRIM mode.
        // In SOLID mode the XML background keeps the root opaque, so clearing it causes
        // the transparent-root / same-as-cards visual bug on re-open.
        if (!isMinusOne && isBlurBackground) setBlurEnabled(false)
        super.onPause()
        viewModel.onPause()
    }

    private fun setBlurEnabled(enabled: Boolean) {
        val bg = monet.getBackgroundColor(requireContext())
        // Overlay mode: SmartspacerOverlay manages SOLID and SCRIM backgrounds, so keep
        // the fragment root transparent in those modes. For BLUR the overlay controls the
        // visual close state via blur radius (zeroed on pause/stop), so we keep the 50%
        // tint permanently — never clearing it on pause — so the drag-open animation
        // always shows the tinted surface rather than jumping in on onResume().
        if (isOverlay) {
            // BLUR/SCRIM: SmartspacerOverlay manages the wallpaper/blur; keep the root
            // semi-transparent so the wallpaper shows through.
            // SOLID: SmartspacerOverlay's background colour is from a service context
            // (no DynamicColors → resolves to white). Use the Activity context here —
            // it has DynamicColors applied and gives the correct Monet colorBackground.
            binding.root.setBackgroundColor(
                if (isBlurBackground) ColorUtils.setAlphaComponent(bg, 192)
                else bg
            )
            return
        }
        // In BLUR mode the root uses 50% opacity so the blurred wallpaper shows through.
        // In all other modes the root is fully opaque (or transparent when disabled/paused).
        val targetAlpha = when {
            !enabled -> 0
            isBlurBackground -> 192
            else -> 255
        }
        binding.root.setBackgroundColor(ColorUtils.setAlphaComponent(bg, targetAlpha))
        if (isBlurBackground) {
            // Ensure FLAG_SHOW_WALLPAPER is set at runtime for both BLUR and SCRIM modes.
            if (enabled) {
                requireActivity().window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
                )
            }
            // Only apply window blur for BLUR mode — SCRIM shows the same transparent
            // surfaces but over an unblurred wallpaper.
            if (isActualBlur) {
                blurProvider.applyBlurToWindow(requireActivity().window, if (enabled) 1f else 0f)
            }
            // Cover the nav bar with an opaque surface so the wallpaper doesn't
            // show through the otherwise-transparent system bar.
            requireActivity().window.navigationBarColor =
                ColorUtils.setAlphaComponent(bg, if (enabled) 255 else 0)
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

    private fun launchOverlayAction(action: OpenFromOverlayAction, navigateToTabSettings: Boolean = false) {
        unlockAndLaunch(ExpandedActivity.createExportedOverlayIntent(requireContext()).apply {
            putExtra(EXTRA_OPEN_ACTION, action)
            if (navigateToTabSettings) putExtra(EXTRA_NAVIGATE_TAB_SETTINGS, true)
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
            val old = targets
            targets = newTargets
            if (old.size != newTargets.size) {
                notifyDataSetChanged()
                return
            }
            // Compare full target content (data class equality), not just ID.
            // This ensures countdown timers and alarm times redraw when their text
            // changes even though the smartspaceTargetId stays the same.
            newTargets.forEachIndexed { i, new ->
                if (new != old.getOrNull(i)) {
                    notifyItemChanged(i)
                }
            }
        }

        inner class ViewHolder(val sv: SmartspacerView, container: FrameLayout)
            : RecyclerView.ViewHolder(container) {
            var boundTarget: SmartspaceTarget? = null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val sv = SmartspacerView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val container = FrameLayout(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(16.dp, 16.dp, 0, 16.dp)
                addView(sv)
            }
            return ViewHolder(sv, container)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val target = targets[position]
            if (target.target.equalsForUi(holder.boundTarget)) return
            holder.boundTarget = target.target
            val tintColour = requireContext()
                .getAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
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

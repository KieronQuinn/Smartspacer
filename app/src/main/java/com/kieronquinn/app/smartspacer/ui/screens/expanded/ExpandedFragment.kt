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
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.getContrastColor
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.isActivityCompat
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.toArgb
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
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
        binding.expandedUnlockContainer.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
        binding.expandedUnlock.overrideRippleColor(monet.getAccentColor(requireContext()))
        binding.expandedUnlock.iconTint =
            ColorStateList.valueOf(monet.getAccentColor(requireContext()))
        binding.expandedDisabledButton.applyMonet()
        binding.expandedPermission.backgroundTintList =
            ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
        // Set pill outline to a high-contrast color so it's visible in both light and dark mode.
        val isSystemDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val strokeColor = if (isSystemDark)
            ColorUtils.setAlphaComponent(Color.WHITE, 120)
        else
            ColorUtils.setAlphaComponent(Color.BLACK, 90)
        binding.expandedHeaderPill.strokeColor = strokeColor
    }

    private fun setupInsets() {
        binding.expandedUnlockContainer.onApplyInsets { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
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

        binding.expandedTabButtons.removeAllViews()
        tabs.forEachIndexed { index, tab ->
            val btn = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = tab.label
                cornerRadius = 100.dp
                setPadding(20.dp, 8.dp, 20.dp, 8.dp)
                textSize = 15f
                isAllCaps = false
                elevation = 0f
                insetTop = 0
                insetBottom = 0
                strokeWidth = 0
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener { selectTab(index) }
            }
            binding.expandedTabButtons.addView(btn)
        }
        selectTab(0)
    }

    private fun setupHeaderSwipe() {
        // ViewPager2 handles finger-tracking and page snapping directly.
        // The pill's SwipeDetectingCardView still fires requestDisallowInterceptTouchEvent on
        // ACTION_DOWN to guard against SlidingPanelLayout in the overlay context, but its own
        // fling-intercept is disabled (onHorizontalSwipe = null) so it never cancels the pager.
        binding.expandedHeaderPill.onHorizontalSwipe = null
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

    private fun selectTab(index: Int) {
        val tabs = tabRepository.getTabs()
        if (index < 0 || index >= tabs.size) return
        currentTabIndex = index
        binding.expandedWidgetFlipper.displayedChild = index

        val selectedBg = monet.getMonetColors().accent1[600]?.toArgb()
            ?: monet.getAccentColor(requireContext())
        val selectedFg = monet.getMonetColors().accent1[50]?.toArgb() ?: Color.WHITE
        val unselectedFg = monet.getBackgroundColor(requireContext()).getContrastColor()

        for (i in 0 until binding.expandedTabButtons.childCount) {
            val btn = binding.expandedTabButtons.getChildAt(i) as? MaterialButton ?: continue
            if (i == index) {
                btn.backgroundTintList = ColorStateList.valueOf(selectedBg)
                btn.setTextColor(selectedFg)
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                btn.setTextColor(unselectedFg)
            }
        }

        if (!loadedTabIndices.contains(index)) {
            loadedTabIndices.add(index)
            loadWidgetForTab(index, tabs[index].appWidgetId)
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
        val isSystemDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val dotColor = if (isSystemDark) Color.WHITE else Color.BLACK
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
            })

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val target = targets[position]
            val isSystemDark = (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val tintColour = if (isSystemDark) Color.WHITE else Color.BLACK
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

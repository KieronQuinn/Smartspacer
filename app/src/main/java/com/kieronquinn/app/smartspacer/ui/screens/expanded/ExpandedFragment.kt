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
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
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
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_ABOUT_INTENT
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_FEEDBACK_INTENT
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.shouldExcludeFromSmartspacer
import com.kieronquinn.app.smartspacer.ui.activities.ExpandedActivity
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.activities.OverlayTrampolineActivity
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.expanded.BaseExpandedAdapter.ExpandedAdapterListener
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession.State
import com.kieronquinn.app.smartspacer.utils.extensions.WIDGET_MIN_COLUMNS
import com.kieronquinn.app.smartspacer.utils.extensions.awaitPost
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.getContrastColor
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetColumnCount
import com.kieronquinn.app.smartspacer.utils.extensions.isActivityCompat
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.runAfterAnimationsFinished
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import com.kieronquinn.app.smartspacer.sdk.client.R as SDKR

class ExpandedFragment: BoundFragment<FragmentExpandedBinding>(
    FragmentExpandedBinding::inflate
), SmartspaceTargetInteractionListener, View.OnScrollChangeListener, ExpandedAdapterListener {

    companion object {
        private const val MIN_SWIPE_DELAY = 250L
        private const val EXTRA_OPEN_ACTION = "open_action"
        private const val EXTRA_OPEN_TARGET = "open_target"

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
            if(launchIntent?.component != COMPONENT_EXPANDED) return null
            val targetId = launchIntent.getParcelableExtraCompat(
                EXTRA_OPEN_ACTION, OpenFromOverlayAction.OpenTarget::class.java
            )?.id ?: return null
            return Intent().apply {
                component = ComponentName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.ui.activities.ExportedExpandedActivity"
                )
                putExtra(EXTRA_OPEN_TARGET, targetId)
            }
        }
    }

    private val isOverlay by lazy {
        ExpandedActivity.isOverlay(requireActivity() as ExpandedActivity)
    }

    private val uid by lazy {
        ExpandedActivity.getUid(requireActivity() as ExpandedActivity)
    }

    private val sessionId by lazy {
        if(isOverlay){
            "overlay"
        }else{
            "expanded"
        }
    }

    private val viewModel by viewModel<ExpandedViewModel> {
        parametersOf("${sessionId}_$uid")
    }

    private val wallpaperRepository by inject<WallpaperRepository>()
    private val blurProvider by inject<BlurProvider>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val adapterUpdateBus = MutableStateFlow<Long?>(null)
    private var lastSwipe: Long? = null
    private var popup: Balloon? = null
    private var topInset = 0
    private var multiColumnEnabled = true

    private val widgetBindResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onWidgetBindResult(
            widgetConfigureResult,
            it.resultCode == Activity.RESULT_OK
        )
    }

    private val widgetConfigureResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.onWidgetConfigureResult(it.resultCode == Activity.RESULT_OK)
    }

    private val isDark = runBlocking {
        wallpaperRepository.homescreenWallpaperDarkTextColour.first()
    }

    private val backgroundColour by lazy {
        monet.getBackgroundColor(requireContext())
    }

    private val adapter by lazy {
        ExpandedAdapter(
            binding.expandedRecyclerView,
            isDark,
            sessionId,
            this,
            this,
            ::getSpanPercent,
            ::getAvailableWidth
        )
    }

    private val keyguardManager by lazy {
        requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val theme = if(isDark){
            R.style.Theme_Smartspacer_Wallpaper_Dark
        }else{
            R.style.Theme_Smartspacer_Wallpaper_Light
        }
        val contextThemeWrapper = ContextThemeWrapper(requireContext(), theme)
        return inflater.cloneInContext(contextThemeWrapper)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowCompat.getInsetsController(requireActivity().window, view).run {
            isAppearanceLightNavigationBars = isDark
            isAppearanceLightStatusBars = isDark
        }
        setupLoading()
        setupState()
        setupMonet()
        setupInsets()
        setupUnlock()
        setupOverlaySwipe()
        setupDisabledButton()
        setupClose()
        handleLaunchActionIfNeeded()
        viewModel.setup(isOverlay)
        setupRecyclerView()
    }

    override fun onDestroyView() {
        binding.expandedRecyclerView.adapter = null
        super.onDestroyView()
    }

    private fun setupLoading() = whenCreated {
        with(binding.expandedLoading) {
            (drawable as AnimatedVectorDrawable).start()
        }
    }

    private fun setupMonet() {
        binding.expandedUnlockContainer.backgroundTintList = ColorStateList.valueOf(
            monet.getBackgroundColor(requireContext())
        )
        binding.expandedUnlock.overrideRippleColor(monet.getAccentColor(requireContext()))
        binding.expandedUnlock.iconTint = ColorStateList.valueOf(
            monet.getAccentColor(requireContext())
        )
        binding.expandedDisabledButton.applyMonet()
        binding.expandedPermission.backgroundTintList = ColorStateList.valueOf(
            monet.getBackgroundColor(requireContext())
        )
    }

    private fun setupInsets() = with(binding) {
        expandedUnlockContainer.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
        }
        root.onApplyInsets { _, insets ->
            topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            viewModel.setTopInset(topInset)
        }
        val lockedPadding = resources.getDimensionPixelSize(R.dimen.expanded_button_unlock_height)
        expandedRecyclerView.onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                .bottom + lockedPadding
            val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val leftInset = cutoutInsets.left
            val rightInset = cutoutInsets.right
            view.updatePadding(
                left = leftInset,
                right = rightInset,
                bottom = bottomInset
            )
        }
    }

    private fun setupRecyclerView() = with(binding.expandedRecyclerView) {
        layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            alignItems = AlignItems.CENTER
            justifyContent = JustifyContent.CENTER
            flexWrap = FlexWrap.WRAP
        }
        adapter = this@ExpandedFragment.adapter
        binding.expandedRecyclerView.itemAnimator = PulseControlledItemAnimator()
        setOnScrollChangeListener(this@ExpandedFragment)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        setBlurEnabled(true)
    }

    override fun onPause() {
        setBlurEnabled(false)
        super.onPause()
        viewModel.onPause()
    }

    private fun setBlurEnabled(enabled: Boolean) {
        if(isOverlay) return //Handled progressively by overlay
        when(settingsRepository.expandedBackground.getSync()) {
            ExpandedBackground.BLUR -> {
                val ratio = if(enabled) 1f else 0f
                blurProvider.applyBlurToWindow(requireActivity().window, ratio)
            }
            ExpandedBackground.SCRIM -> {
                val alpha = if(enabled) 128 else 0
                val backgroundColour = ColorUtils.setAlphaComponent(Color.BLACK, alpha)
                binding.root.setBackgroundColor(backgroundColour)
            }
            ExpandedBackground.SOLID -> {
                val alpha = if(enabled) 255 else 0
                val backgroundColour = ColorUtils.setAlphaComponent(backgroundColour, alpha)
                binding.root.setBackgroundColor(backgroundColour)
            }
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
                binding.expandedLoading.isVisible = true
                binding.expandedRecyclerView.isVisible = false
                binding.expandedUnlockContainer.isVisible = false
                binding.expandedDisabled.isVisible = false
                binding.expandedPermission.isVisible = false
            }
            is State.Disabled -> {
                binding.expandedLoading.isVisible = false
                binding.expandedRecyclerView.isVisible = false
                binding.expandedUnlockContainer.isVisible = false
                binding.expandedDisabled.isVisible = true
                binding.expandedPermission.isVisible = false
            }
            is State.PermissionRequired -> {
                binding.expandedLoading.isVisible = false
                binding.expandedRecyclerView.isVisible = false
                binding.expandedUnlockContainer.isVisible = false
                binding.expandedDisabled.isVisible = false
                binding.expandedPermission.isVisible = true
            }
            is State.Loaded -> {
                multiColumnEnabled = state.multiColumnEnabled
                binding.expandedLoading.isVisible = false
                binding.expandedRecyclerView.isVisible = true
                binding.expandedUnlockContainer.isVisible = state.isLocked && !isOverlay
                binding.expandedDisabled.isVisible = false
                binding.expandedPermission.isVisible = false
                setStatusBarLight(state.lightStatusIcons)
                whenResumed {
                    binding.expandedRecyclerView.runAfterAnimationsFinished {
                        adapter.submitList(state.items) {
                            try {
                                whenCreated {
                                    adapterUpdateBus.emit(System.currentTimeMillis())
                                }
                            }catch (e: IllegalStateException) {
                                //Overlay has been killed
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setStatusBarLight(enabled: Boolean) {
        WindowCompat.getInsetsController(requireActivity().window, requireView())
            .isAppearanceLightStatusBars = enabled
    }

    private fun setupUnlock() = with(binding.expandedUnlock) {
        viewLifecycleOwner.whenResumed {
            onClicked().collect {
                unlockAndLaunch(null)
            }
        }
    }

    private fun setupOverlaySwipe() = viewLifecycleOwner.whenResumed {
        viewModel.overlayDrag.collect {
            lastSwipe = System.currentTimeMillis()
            popup?.dismiss()
            popup = null
        }
    }

    private fun setupDisabledButton() = with(binding.expandedDisabledButton) {
        viewLifecycleOwner.whenResumed {
            onClicked().collect {
                Intent(requireContext(), MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("smartspacer://expanded")
                    putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
                }.also { intent ->
                    startActivity(intent)
                }
            }
        }
    }

    private fun setupClose() = viewLifecycleOwner.whenResumed {
        viewModel.exitBus.collect {
            if(it && !isOverlay) {
                requireActivity().finishAndRemoveTask()
            }
        }
    }

    private fun handleLaunchActionIfNeeded() = whenResumed {
        val action = getAndClearOverlayAction()
            ?: getAndClearOverlayTarget() ?: return@whenResumed
        //Await an adapter update if needed
        adapterUpdateBus.first {
            adapter.currentList.isNotEmpty()
        }
        binding.expandedRecyclerView.awaitPost()
        binding.expandedNestedScroll.scrollTo(0, action.scrollPosition)
        when(action){
            is OpenFromOverlayAction.ConfigureWidget -> {
                onConfigureWidgetClicked(action.info, action.id, action.config)
            }
            is OpenFromOverlayAction.AddWidget -> {
                onAddWidgetClicked()
            }
            is OpenFromOverlayAction.Rearrange -> {
                viewModel.onRearrangeClicked()
            }
            is OpenFromOverlayAction.OpenTarget -> {
                val itemPosition = adapter.currentList.indexOfFirst {
                    it is Item.Target && it.target.smartspaceTargetId == action.id
                }
                if(itemPosition < 0) return@whenResumed
                val itemView = binding.expandedRecyclerView
                    .findViewHolderForAdapterPosition(itemPosition)?.itemView
                    ?: return@whenResumed
                val scrollTo = (itemView.top - topInset).coerceAtLeast(0)
                binding.expandedNestedScroll.scrollTo(0, scrollTo)
            }
            is OpenFromOverlayAction.Options -> {
                viewModel.onOptionsClicked(action.appWidgetId, action.canReconfigure)
            }
        }
    }

    override fun onSearchLensClicked(searchApp: SearchApp) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google://lens")
            component = ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.apps.search.lens.LensExportedActivity"
            )
            putExtra("LensHomescreenShortcut", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        unlockAndLaunch(intent)
    }

    override fun onSearchMicClicked(searchApp: SearchApp) {
        val block = {
            startActivity(Intent(Intent.ACTION_VOICE_COMMAND).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        if(searchApp.requiresUnlock){
            unlockAndInvoke(block)
        }else{
            block()
        }
    }

    override fun onDoodleClicked(doodleImage: DoodleImage) {
        unlockAndInvoke {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(doodleImage.searchUrl ?: return@apply)
            })
        }
    }

    override fun onSearchBoxClicked(searchApp: SearchApp) {
        unlockAndInvoke {
            startActivity(searchApp.launchIntent)
        }
    }

    private fun unlockAndLaunch(intent: Intent?) {
        unlockAndInvoke {
            try {
                startActivity(intent ?: return@unlockAndInvoke)
            }catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    SDKR.string.smartspace_long_press_popup_failed_to_launch,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun unlockAndInvoke(block: () -> Unit) {
        if(!isAdded) return
        if(!keyguardManager.isKeyguardLocked){
            block()
            return
        }
        keyguardManager.requestDismissKeyguard(
            requireActivity(),
            object: KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    block()
                }
            }
        )
    }

    override fun onInteraction(target: SmartspaceTarget, actionId: String?) {
        viewModel.onTargetInteraction(target, actionId)
    }

    override fun onLongPress(target: SmartspaceTarget): Boolean {
        val canDismiss = target.canBeDismissed &&
                target.featureType != SmartspaceTarget.FEATURE_WEATHER
        val aboutIntent = target.baseAction?.extras
            ?.getParcelableCompat(KEY_EXTRA_ABOUT_INTENT, Intent::class.java)
            ?.takeIf { !it.shouldExcludeFromSmartspacer() }
        val feedbackIntent = target.baseAction?.extras
            ?.getParcelableCompat(KEY_EXTRA_FEEDBACK_INTENT, Intent::class.java)
            ?.takeIf { !it.shouldExcludeFromSmartspacer() }
        if(!canDismiss && aboutIntent == null && feedbackIntent == null) return false
        val position = adapter.currentList.indexOfFirst { item ->
            item is Item.Target && item.target == target
        }
        if(position == -1) return false
        val holder = binding.expandedRecyclerView.findViewHolderForAdapterPosition(position)
            ?: return false
        return showPopup(holder.itemView, target, canDismiss, aboutIntent, feedbackIntent)
    }

    override fun launch(unlock: Boolean, block: () -> Unit) {
        if(unlock){
            unlockAndInvoke(block)
        }else block()
    }

    override fun onConfigureWidgetClicked(
        info: AppWidgetProviderInfo,
        id: String?,
        config: CustomExpandedAppWidgetConfig?
    ) {
        if(isOverlay){
            launchOverlayAction(OpenFromOverlayAction.ConfigureWidget(info, id, config, getScroll()))
        }else{
            unlockAndInvoke {
                viewModel.onConfigureWidgetClicked(
                    widgetBindResult,
                    widgetConfigureResult,
                    info,
                    id,
                    config
                )
            }
        }
    }

    override fun onAddWidgetClicked() {
        if(isOverlay){
            launchOverlayAction(OpenFromOverlayAction.AddWidget(getScroll()))
        }else{
            unlockAndInvoke {
                viewModel.onAddWidgetClicked()
            }
        }
    }

    override fun onShortcutClicked(shortcut: Shortcut) {
        if(shortcut.pendingIntent?.isActivityCompat() == true){
            viewModel.onShortcutClicked(requireContext(), shortcut)
        }else{
            unlockAndInvoke {
                viewModel.onShortcutClicked(requireContext(), shortcut)
            }
        }
    }

    override fun onAppShortcutClicked(appShortcut: AppShortcut) {
        unlockAndInvoke {
            viewModel.onAppShortcutClicked(appShortcut)
        }
    }

    override fun onWidgetLongClicked(viewHolder: RecyclerView.ViewHolder, appWidgetId: Int?) {
        if(appWidgetId == null) return
        val view = viewHolder.itemView
        lastSwipe?.let {
            if(System.currentTimeMillis() - it < MIN_SWIPE_DELAY){
                return //Likely a swipe
            }
        }
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, 0)
        val popupView = SmartspaceExpandedLongPressPopupWidgetBinding.inflate(layoutInflater)
        val background = requireContext().getAttrColor(android.R.attr.colorBackground)
        val textColour = background.getContrastColor()
        val popup = Balloon.Builder(requireContext())
            .setLayout(popupView)
            .setHeight(BalloonSizeSpec.WRAP)
            .setWidthResource(SDKR.dimen.smartspace_long_press_popup_width)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setBackgroundColor(background)
            .setArrowColor(background)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setCornerRadius(16f)
            .setBalloonAnimation(BalloonAnimation.FADE)
            .build()
        popup.showAlignBottom(view)
        popupView.expandedLongPressPopupReset.setTextColor(textColour)
        popupView.expandedLongPressPopupReset.iconTint = ColorStateList.valueOf(textColour)
        popupView.expandedLongPressPopupReset.setOnClickListener {
            popup.dismiss()
            unlockAndInvoke {
                viewModel.onAppWidgetReset(appWidgetId)
            }
        }
        this.popup = popup
    }

    override fun onWidgetDeleteClicked(widget: Item.RemovedWidget) {
        viewModel.onDeleteCustomWidget(widget.appWidgetId ?: return)
    }

    private fun showPopup(
        view: View,
        target: SmartspaceTarget,
        canDismiss: Boolean,
        aboutIntent: Intent?,
        feedbackIntent: Intent?
    ): Boolean {
        lastSwipe?.let {
            if(System.currentTimeMillis() - it < MIN_SWIPE_DELAY){
                return false //Likely a swipe
            }
        }
        val popupView = SmartspaceExpandedLongPressPopupBinding.inflate(layoutInflater)
        val background = requireContext().getAttrColor(android.R.attr.colorBackground)
        val textColour = background.getContrastColor()
        val popup = Balloon.Builder(requireContext())
            .setLayout(popupView)
            .setHeight(BalloonSizeSpec.WRAP)
            .setWidthResource(SDKR.dimen.smartspace_long_press_popup_width)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setBackgroundColor(background)
            .setArrowColor(background)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setCornerRadius(16f)
            .setBalloonAnimation(BalloonAnimation.FADE)
            .build()
        popup.showAlignBottom(view)
        popupView.smartspaceLongPressPopupAbout.isVisible = aboutIntent != null
        popupView.smartspaceLongPressPopupAbout.setTextColor(textColour)
        popupView.smartspaceLongPressPopupAbout.iconTint = ColorStateList.valueOf(textColour)
        popupView.smartspaceLongPressPopupAbout.setOnClickListener {
            popup.dismiss()
            unlockAndLaunch(aboutIntent)
        }
        popupView.smartspaceLongPressPopupFeedback.isVisible = feedbackIntent != null
        popupView.smartspaceLongPressPopupFeedback.setTextColor(textColour)
        popupView.smartspaceLongPressPopupFeedback.iconTint = ColorStateList.valueOf(textColour)
        popupView.smartspaceLongPressPopupFeedback.setOnClickListener {
            popup.dismiss()
            unlockAndLaunch(feedbackIntent)
        }
        popupView.smartspaceLongPressPopupDismiss.isVisible = canDismiss
        popupView.smartspaceLongPressPopupDismiss.setTextColor(textColour)
        popupView.smartspaceLongPressPopupDismiss.iconTint = ColorStateList.valueOf(textColour)
        popupView.smartspaceLongPressPopupDismiss.setOnClickListener {
            popup.dismiss()
            viewModel.onTargetDismiss(target)
        }
        this.popup = popup
        return true
    }

    override fun onCustomWidgetLongClicked(view: View, widget: Item.Widget) {
        lastSwipe?.let {
            if(System.currentTimeMillis() - it < MIN_SWIPE_DELAY){
                return //Likely a swipe
            }
        }
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, 0)
        val popupView = SmartspaceExpandedLongPressPopupCustomWidgetBinding.inflate(layoutInflater)
        val background = requireContext().getAttrColor(android.R.attr.colorBackground)
        val textColour = background.getContrastColor()
        val popup = Balloon.Builder(requireContext())
            .setLayout(popupView)
            .setHeight(BalloonSizeSpec.WRAP)
            .setWidthResource(SDKR.dimen.smartspace_long_press_popup_width)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setBackgroundColor(background)
            .setArrowColor(background)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setCornerRadius(16f)
            .setBalloonAnimation(BalloonAnimation.FADE)
            .build()
        popup.showAlignBottom(view)
        popupView.expandedLongPressPopupDelete.setTextColor(textColour)
        popupView.expandedLongPressPopupDelete.iconTint = ColorStateList.valueOf(textColour)
        popupView.expandedLongPressPopupDelete.setOnClickListener {
            popup.dismiss()
            unlockAndInvoke {
                viewModel.onDeleteCustomWidget(
                    widget.appWidgetId ?: return@unlockAndInvoke
                )
            }
        }
        popupView.expandedLongPressPopupOptions.setTextColor(textColour)
        popupView.expandedLongPressPopupOptions.iconTint = ColorStateList.valueOf(textColour)
        popupView.expandedLongPressPopupOptions.setOnClickListener {
            popup.dismiss()
            val appWidgetId = widget.appWidgetId ?: return@setOnClickListener
            val canReconfigure = widget.provider.canReconfigure()
            if(isOverlay){
                launchOverlayAction(
                    OpenFromOverlayAction.Options(getScroll(), appWidgetId, canReconfigure)
                )
            }else{
                unlockAndInvoke {
                    viewModel.onOptionsClicked(appWidgetId, canReconfigure)
                }
            }
        }
        popupView.expandedLongPressPopupRearrange.setTextColor(textColour)
        popupView.expandedLongPressPopupRearrange.iconTint = ColorStateList.valueOf(textColour)
        popupView.expandedLongPressPopupRearrange.setOnClickListener {
            popup.dismiss()
            if(isOverlay){
                val appWidgetId = widget.appWidgetId ?: return@setOnClickListener
                launchOverlayAction(OpenFromOverlayAction.Rearrange(appWidgetId))
            }else {
                unlockAndInvoke {
                    viewModel.onRearrangeClicked()
                }
            }
        }
        this.popup = popup
    }

    override fun onScrollChange(
        view: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int
    ) {
        lastSwipe = System.currentTimeMillis()
        popup?.dismiss()
        popup = null
    }

    override fun shouldTrampolineLaunches(): Boolean = isOverlay

    override fun trampolineLaunch(view: View, pendingIntent: PendingIntent) {
        OverlayTrampolineActivity.trampoline(view, requireContext(), pendingIntent)
    }

    private fun getAndClearOverlayAction(): OpenFromOverlayAction? {
        return requireActivity().intent.run {
            getParcelableExtraCompat(EXTRA_OPEN_ACTION, OpenFromOverlayAction::class.java).also {
                removeExtra(EXTRA_OPEN_ACTION)
            }
        }
    }

    private fun getAndClearOverlayTarget(): OpenFromOverlayAction.OpenTarget? {
        return requireActivity().intent.run {
            getStringExtra(EXTRA_OPEN_TARGET).also {
                removeExtra(EXTRA_OPEN_TARGET)
            }?.let {
                OpenFromOverlayAction.OpenTarget(it)
            }
        }
    }

    private fun launchOverlayAction(action: OpenFromOverlayAction) {
        val intent = ExpandedActivity.createExportedOverlayIntent(requireContext()).apply {
            putExtra(EXTRA_OPEN_ACTION, action)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        unlockAndLaunch(intent)
    }

    private fun getScroll() = with(binding.expandedNestedScroll) {
        scrollY
    }

    private fun AppWidgetProviderInfo.canReconfigure(): Boolean {
        return configure != null && widgetFeatures and WIDGET_FEATURE_RECONFIGURABLE != 0
    }

    sealed class OpenFromOverlayAction(open val scrollPosition: Int): Parcelable {
        @Parcelize
        data class OpenTarget(val id: String): OpenFromOverlayAction(0)
        @Parcelize
        data class ConfigureWidget(
            val info: AppWidgetProviderInfo,
            val id: String?,
            val config: CustomExpandedAppWidgetConfig?,
            override val scrollPosition: Int
        ): OpenFromOverlayAction(scrollPosition)
        @Parcelize
        data class AddWidget(override val scrollPosition: Int):
            OpenFromOverlayAction(scrollPosition)
        @Parcelize
        data class Rearrange(override val scrollPosition: Int):
            OpenFromOverlayAction(scrollPosition)
        @Parcelize
        data class Options(
            override val scrollPosition: Int, val appWidgetId: Int, val canReconfigure: Boolean
        ): OpenFromOverlayAction(scrollPosition)
    }

    private class PulseControlledItemAnimator: DefaultItemAnimator() {

        override fun animateChange(
            oldHolder: ViewHolder,
            newHolder: ViewHolder,
            fromX: Int,
            fromY: Int,
            toX: Int,
            toY: Int
        ): Boolean {
            val isMove = fromX != toX || fromY != toY
            if(!isMove && shouldOverride(oldHolder, newHolder)) {
                dispatchChangeStarting(oldHolder, true)
                dispatchChangeStarting(newHolder, false)
                oldHolder.itemView.alpha = 0f
                newHolder.itemView.alpha = 1f
                dispatchChangeFinished(oldHolder, true)
                dispatchChangeFinished(newHolder, false)
                return true
            }
            return super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY)
        }

        private fun shouldOverride(oldHolder: ViewHolder, newHolder: ViewHolder): Boolean {
            if(oldHolder !is BaseExpandedAdapter.ViewHolder.Target) return false
            if(newHolder !is BaseExpandedAdapter.ViewHolder.Target) return false
            return oldHolder.adapterPosition == newHolder.adapterPosition
        }

    }

    private fun getAvailableWidth(): Int {
        return binding.expandedRecyclerView.measuredWidth - 16.dp
    }

    private fun getSpanPercent(item: Item): Float {
        var columnCount = requireContext().getWidgetColumnCount(getAvailableWidth())
        if(!multiColumnEnabled) {
            //Prevent widgets being displayed alongside each other when multi column is disabled
            columnCount = columnCount.coerceAtMost(WIDGET_MIN_COLUMNS)
        }
        val targetBasedColumns = if(multiColumnEnabled) {
            (columnCount / WIDGET_MIN_COLUMNS.toFloat()).coerceAtLeast(1f)
        }else 1f
        val targetBasedWidth = (1f / targetBasedColumns)
        return when(item) {
            is Item.StatusBarSpace -> 1f
            is Item.Search -> 1f
            is Item.Target -> targetBasedWidth
            is Item.Complications -> 1f
            is Item.Widget -> {
                return when {
                    item.fullWidth -> targetBasedWidth
                    item.spanX != null -> {
                        item.spanX / columnCount.toFloat()
                    }
                    else -> targetBasedWidth //Unlikely to expect being full width when wide
                }
            }
            is Item.RemovedWidget -> targetBasedWidth
            is Item.RemoteViews -> targetBasedWidth //Unlikely to expect being full width when wide
            is Item.Shortcuts -> 1f
            is Item.Footer -> 1f
            is Item.Spacer -> 1f //Always full width to force new rows
        }
    }

}
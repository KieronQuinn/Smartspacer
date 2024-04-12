package com.kieronquinn.app.smartspacer.ui.screens.configuration.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.shape.CornerFamily
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.ListWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSessionState.DotConfig
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerPage
import com.kieronquinn.app.smartspacer.databinding.FragmentWidgetConfigurationBinding
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Card
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Dropdown
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Header
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Slider
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.SwitchSetting
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.configuration.widget.WidgetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.collapsedState
import com.kieronquinn.app.smartspacer.utils.extensions.expandProgress
import com.kieronquinn.app.smartspacer.utils.extensions.getRememberedAppBarCollapsed
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onSelected
import com.kieronquinn.app.smartspacer.utils.extensions.rememberAppBarCollapsed
import com.kieronquinn.app.smartspacer.utils.extensions.selectTab
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.toArgb
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt
import android.graphics.drawable.Icon as AndroidIcon

class WidgetConfigurationFragment: BoundFragment<FragmentWidgetConfigurationBinding>(FragmentWidgetConfigurationBinding::inflate), BackAvailable, ProvidesBack, LockCollapsed {

    companion object {
        const val EXTRA_CALLING_PACKAGE = "calling_package"
    }

    private val adapter by lazy {
        Adapter()
    }

    private val toolbarColour by lazy {
        monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
    }

    private val backgroundColour by lazy {
        monet.getBackgroundColor(requireContext())
    }

    private val enableAccessibilityLabel by lazy {
        SpannableStringBuilder().apply {
            append(
                getString(R.string.widget_configuration_accessibility_title),
                StyleSpan(Typeface.BOLD),
                SPAN_EXCLUSIVE_EXCLUSIVE
            )
            appendLine()
            append(getString(R.string.widget_configuration_accessibility_content))
            appendLine()
            append(
                getString(R.string.widget_configuration_accessibility_enable),
                StyleSpan(Typeface.BOLD),
                SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private val mockTarget by lazy {
        TargetTemplate.Basic(
            "1",
            ComponentName(requireContext(), WidgetConfigurationFragment::class.java),
            title = Text(getString(R.string.widget_configuration_mock_target_title)),
            subtitle = Text(getString(R.string.widget_configuration_mock_target_subtitle)),
            icon = Icon(AndroidIcon.createWithResource(requireContext(), R.drawable.ic_targets)),
            subComplication = ComplicationTemplate.Basic(
                "1",
                Icon(AndroidIcon.createWithResource(requireContext(), R.drawable.ic_complications)),
                Text(getString(R.string.widget_configuration_mock_complication_title)),
                onClick = null
            ).create()
        ).create()
    }

    private val viewModel by viewModel<WidgetConfigurationViewModel>()
    private var previousState: State? = null

    override val backIcon = R.drawable.ic_check_tinted

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupLoading()
        setupMonet()
        setupCard()
        setupCollapsedState()
        setupTabs()
        setupState()
        val intent = requireActivity().intent
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val calling = requireActivity().callingPackage
            ?: getFallbackCallingPackage() ?: ""
        if(appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
            requireActivity().setResult(Activity.RESULT_CANCELED, Intent())
            requireActivity().finish()
            return
        }
        viewModel.setup(appWidgetId, calling)
        binding.widgetConfigurationAppBar.setExpanded(!getRememberedAppBarCollapsed())
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun getFallbackCallingPackage() = with(requireActivity().intent) {
        if(hasExtra(EXTRA_CALLING_PACKAGE)) {
            verifySecurity()
            getStringExtra(EXTRA_CALLING_PACKAGE)
        }else null
    }

    override fun onBackPressed(): Boolean {
        viewModel.commitAndClose()
        return true
    }

    private fun setupMonet() {
        val tabBackground = monet.getMonetColors().accent1[600]?.toArgb()
            ?: monet.getAccentColor(requireContext(), false)
        binding.widgetConfigurationTabs.backgroundTintList = ColorStateList.valueOf(tabBackground)
        binding.widgetConfigurationTabs.setSelectedTabIndicatorColor(monet.getAccentColor(requireContext()))
        binding.widgetConfigurationBackground.setBackgroundColor(monet.getBackgroundColor(requireContext()))
    }

    private fun setupCard() = with(binding.widgetConfigurationTabsContainer) {
        binding.widgetConfigurationCardView.setBackgroundColor(backgroundColour)
        val roundedCornerSize = resources.getDimension(R.dimen.margin_16)
        whenResumed {
            binding.widgetConfigurationAppBar.expandProgress().collect {
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, roundedCornerSize * it)
                    .setTopRightCorner(CornerFamily.ROUNDED, roundedCornerSize * it)
                    .let {  builder ->
                        if(hasGrantedAccessibility()) {
                            builder.setBottomLeftCorner(CornerFamily.ROUNDED, roundedCornerSize)
                                .setBottomRightCorner(CornerFamily.ROUNDED, roundedCornerSize)
                        }else builder
                    }.build()
            }
        }
    }

    private fun setupCollapsedState() = whenResumed {
        binding.widgetConfigurationAppBar.collapsedState().collect {
            rememberAppBarCollapsed(it)
        }
    }

    private fun setupTabs() = with(binding.widgetConfigurationTabs) {
        whenResumed {
            onSelected().collect {
                viewModel.onTabChanged(it)
            }
        }
    }

    private fun setupRecyclerView() = with(binding.widgetConfigurationContentRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@WidgetConfigurationFragment.adapter
        val additionalPadding = resources.getDimensionPixelSize(R.dimen.margin_16)
        onApplyInsets { _, insets ->
            val bottomInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.ime()).bottom
            updatePadding(bottom = bottomInsets + additionalPadding)
        }
    }

    private fun setupLoading() = with(binding.widgetConfigurationContentLoading.loadingProgress) {
        applyMonet()
    }

    private fun AppWidget.createHorizontalSessionState(): PagedWidgetSmartspacerSessionState {
        return PagedWidgetSmartspacerSessionState(
            WidgetSmartspacerPage(
                SmartspaceRepository.SmartspacePageHolder(mockTarget, null, emptyList()),
                SmartspaceView.ViewType.TEMPLATE_BASIC,
                SmartspaceView.fromTarget(mockTarget, UiSurface.HOMESCREEN, false),
                null
            ),
            SmartspaceConfig(1, UiSurface.HOMESCREEN, requireContext().packageName),
            animate = animate,
            isFirst = true,
            isLast = !multiPage,
            isOnlyPage = !multiPage,
            showControls = showControls,
            invisibleControls = hideControls,
            dotConfig = if(multiPage) {
                listOf(DotConfig.HIGHLIGHTED, DotConfig.REGULAR, DotConfig.REGULAR)
            }else emptyList()
        )
    }

    private fun AppWidget.createVerticalSessionState(): ListWidgetSmartspacerSessionState {
        return ListWidgetSmartspacerSessionState(
            WidgetSmartspacerPage(
                SmartspaceRepository.SmartspacePageHolder(mockTarget, null, emptyList()),
                SmartspaceView.ViewType.TEMPLATE_BASIC,
                SmartspaceView.fromTarget(mockTarget, UiSurface.HOMESCREEN, false),
                null
            ).let {
                listOf(it, it, it)
            },
            SmartspaceConfig(1, UiSurface.HOMESCREEN, requireContext().packageName),
            this
        )
    }

    private fun setupHorizontalPreview(
        config: AppWidget
    ) = with(binding.widgetConfigurationPreviewHorizontal) {
        removeAllViews()
        val state = config.createHorizontalSessionState()
        val preview = with(viewModel) {
            context.getPagedWidget(config.appWidgetId, state, config)
        }
        preview?.apply(context.applicationContext, this)?.let {
            addView(it)
        }
    }

    private fun setupVerticalPreview(
        config: AppWidget
    ) = with(binding.widgetConfigurationPreviewVertical) {
        removeAllViews()
        val state = config.createVerticalSessionState()
        val preview = with(viewModel) {
            context.getListWidget(config)
        }
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        preview.apply(context.applicationContext, this)?.let {
            val adapter = ListAdapter(it.context, config, state)
            it.findViewById<ListView>(R.id.widget_list).run {
                this.adapter = adapter
                updatePadding(top = padding, bottom = padding)
                clipToPadding = false
            }
            addView(it)
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

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> {
                widgetConfigurationContentLoading.root.isVisible = true
                widgetConfigurationContentRecyclerview.isVisible = false
            }
            is State.Loaded -> {
                widgetConfigurationContentLoading.root.isVisible = false
                widgetConfigurationContentRecyclerview.isVisible = true
                widgetConfigurationTabs.selectTab(if(state.widget.listMode) 1 else 0)
                widgetConfigurationPreview.displayedChild = if(state.widget.listMode) 1 else 0
                if(state.hasGrantedAccessibility) {
                    requireActivity().setResult(Activity.RESULT_OK, Intent())
                    widgetConfigurationTabs.isVisible = true
                    widgetConfigurationBackground.isVisible = true
                    widgetConfigurationSpacer.isVisible = false
                    widgetConfigurationContentRecyclerview.updatePadding(
                        top = resources.getDimensionPixelSize(R.dimen.margin_8)
                    )
                    widgetConfigurationTabsContainer.setCardBackgroundColor(toolbarColour)
                }else{
                    widgetConfigurationContentRecyclerview.updatePadding(top = 0)
                    widgetConfigurationTabs.isVisible = false
                    widgetConfigurationBackground.isVisible = false
                    widgetConfigurationSpacer.isVisible = true
                    widgetConfigurationTabsContainer.setCardBackgroundColor(backgroundColour)
                }
                //Block touches on horizontal mode
                widgetConfigurationPreviewBlocker.isVisible = !state.widget.listMode
                setupHorizontalPreview(state.widget)
                setupVerticalPreview(state.widget)
                if(previousState == null || !state.equalsForUi(previousState)) {
                    adapter.update(state.loadItems(), widgetConfigurationContentRecyclerview)
                }
            }
        }
        previousState = state
    }

    private fun hasGrantedAccessibility(): Boolean {
        return (viewModel.state.value as? State.Loaded)?.hasGrantedAccessibility ?: false
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        if(!hasGrantedAccessibility) {
            return listOf(
                Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning),
                    enableAccessibilityLabel,
                    onClick = {
                        viewModel.onAccessibilityClicked(requireContext())
                    }
                )
            )
        }
        return listOfNotNull(
            Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                getText(R.string.widget_configuration_horizontal_info)
            ).takeIf { !widget.listMode },
            Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                getText(R.string.widget_configuration_vertical_info)
            ).takeIf { widget.listMode },
            Header(getString(R.string.widget_configuration_behaviour_title)),
            SwitchSetting(
                isLockScreenAvailable && widget.surface == UiSurface.LOCKSCREEN,
                getString(R.string.widget_configuration_is_lock_screen_title),
                if(isLockScreenAvailable) {
                    getText(R.string.widget_configuration_is_lock_screen_content)
                }else{
                    getText(R.string.widget_configuration_is_lock_screen_content_disabled)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_show_on_lockscreen),
                enabled = isLockScreenAvailable,
                onChanged = viewModel::onSurfaceChanged
            ),
            SwitchSetting(
                !widget.multiPage,
                getString(R.string.widget_configuration_page_single_title),
                getString(R.string.widget_configuration_page_single_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_multi_column),
                onChanged = viewModel::onSinglePageChanged
            ).takeIf { !widget.listMode },
            Header(getString(R.string.widget_configuration_customisation_title)),
            SwitchSetting(
                widget.showControls,
                getString(R.string.widget_configuration_page_arrows_title),
                getString(R.string.widget_configuration_page_arrows_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrows),
                onChanged = viewModel::onShowControlsChanged
            ).takeIf { !widget.listMode && widget.multiPage },
            SwitchSetting(
                widget.hideControls,
                getString(R.string.widget_configuration_page_controls_invisible_title),
                getString(R.string.widget_configuration_page_controls_invisible_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_widget_configuration_controls_invisible),
                onChanged = viewModel::onInvisibleControlsChanged
            ),
            SwitchSetting(
                widget.animate,
                getString(R.string.widget_configuration_animation_switch_title),
                getString(R.string.widget_configuration_animation_switch_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_animation),
                onChanged = viewModel::onAnimationChanged
            ).takeIf { !widget.listMode && widget.multiPage },
            Slider(
                widget.padding.toFloat(),
                0f,
                16f,
                1f,
                getString(R.string.widget_configuration_padding_title),
                getString(R.string.widget_configuration_padding_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_widget_configuration_padding),
                {
                    it.roundToInt().toString()
                }
            ) {
                viewModel.onPaddingChanged(it.roundToInt())
            },
            Dropdown(
                getString(R.string.widget_configuration_colour),
                getString(widget.tintColour.label),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_tint_colour),
                widget.tintColour,
                viewModel::onTintColourChanged,
                TintColour.entries
            ) {
                it.label
            },
            SwitchSetting(
                widget.showShadow,
                getString(R.string.widget_configuration_shadow_title),
                getString(R.string.widget_configuration_shadow_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_shadow),
                onChanged = viewModel::onShadowChanged
            ).takeIf {
                widget.tintColour == TintColour.AUTOMATIC || widget.tintColour == TintColour.WHITE
            }
        )
    }

    inner class Adapter: BaseSettingsAdapter(
        binding.widgetConfigurationContentRecyclerview, emptyList()
    )

    private inner class ListAdapter(
        private val context: Context,
        private val widgetConfig: AppWidget,
        private val state: ListWidgetSmartspacerSessionState
    ): ArrayAdapter<WidgetSmartspacerPage>(requireContext(), 0, state.pages) {
        override fun getCount(): Int {
            return state.pages.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = state.pages.getOrNull(position) ?: return View(requireContext())
            return item.view.load(widgetConfig).apply(context, parent)
        }

        private fun SmartspaceView.load(widget: AppWidget): RemoteViews {
            return with(viewModel) {
                context.getPageRemoteViews(
                    widget.appWidgetId,
                    this@load,
                    widget,
                    true,
                    Intent()
                )
            }
        }
    }

}
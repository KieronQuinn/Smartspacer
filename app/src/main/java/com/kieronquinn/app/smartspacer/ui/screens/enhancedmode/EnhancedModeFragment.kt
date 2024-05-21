package com.kieronquinn.app.smartspacer.ui.screens.enhancedmode

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentEnhancedModeBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.EnhancedModeViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class EnhancedModeFragment: BoundFragment<FragmentEnhancedModeBinding>(FragmentEnhancedModeBinding::inflate), BackAvailable, ProvidesBack {

    private val viewModel by viewModel<EnhancedModeViewModel>()
    private val args by navArgs<EnhancedModeFragmentArgs>()

    private val googleSansText by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text)!!
    }

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupRecyclerView()
        setupLoading()
        setupControls()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun setupRecyclerView() = with(binding.enhancedRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@EnhancedModeFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupLoading() = with(binding.enhancedLoadingProgress) {
        applyMonet()
    }

    private fun setupControls() {
        binding.setupEnhancedControls.isVisible = args.isSetup
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupEnhancedControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupEnhancedControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupEnhancedControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupEnhancedControlsNext.onClicked().collect {
                viewModel.onSkipClicked()
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.enhancedLoading.isVisible = true
                binding.enhancedRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.enhancedLoading.isVisible = false
                binding.enhancedRecyclerView.isVisible = true
                adapter.update(state.loadItems(), binding.enhancedRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return listOf(
            GenericSettingsItem.Switch(
                enabled,
                getSwitchText(),
                ::onSwitchChanged
            ),
            GenericSettingsItem.Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                compatibilityState.getEnabledContentList()
            )
        )
    }

    private fun onSwitchChanged(enabled: Boolean) {
        viewModel.onSwitchClicked(requireContext(), args.isSetup)
    }

    private fun getSwitchText() = SpannableStringBuilder().apply {
        appendLine(getString(R.string.enhanced_mode_enable_title))
        val spannable = SpannableString(getString(R.string.enhanced_mode_enable_subtitle))
        spannable.setSpan(
            TypefaceSpan(googleSansText),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        spannable.setSpan(
            RelativeSizeSpan(0.75f),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        append(spannable)
    }

    private fun CompatibilityState.getEnabledContentList() = SpannableStringBuilder().apply {
        appendLine(getText(R.string.enhanced_mode_enable_content_header))
        if(systemSupported){
            appendLine(getString(R.string.enhanced_mode_enable_content_system))
        }
        if(anyLauncherSupported){
            appendLine(getString(R.string.enhanced_mode_enable_content_native_launcher))
        }
        if(lockscreenSupported){
            appendLine(getString(R.string.enhanced_mode_enable_content_native_systemui))
        }
        appendLine(getString(R.string.enhanced_mode_enable_content_oem_smartspace))
        if(appPredictionSupported){
            appendLine(getString(R.string.enhanced_mode_enable_content_app_prediction))
        }
        appendLine(getString(R.string.enhanced_mode_enable_content_recent_apps))
    }.trim()

    override fun interceptBack(): Boolean {
        return args.isSetup
    }

    override fun onBackPressed(): Boolean = viewModel.onBackPressed(args.isSetup)

    inner class Adapter: BaseSettingsAdapter(binding.enhancedRecyclerView, emptyList())

}
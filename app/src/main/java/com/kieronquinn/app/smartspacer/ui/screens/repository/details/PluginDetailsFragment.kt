package com.kieronquinn.app.smartspacer.ui.screens.repository.details

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentPluginDetailsBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.repositories.PluginApi.RemotePluginInfo
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesTitle
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModel.PluginViewState
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModel.PluginViewState.IncompatibleReason
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.updateDisplayedChild
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.utils.glide.SystemIconShapeTransformation
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import io.noties.markwon.Markwon
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class PluginDetailsFragment: BoundFragment<FragmentPluginDetailsBinding>(FragmentPluginDetailsBinding::inflate), BackAvailable, LockCollapsed, ProvidesTitle {

    private val navArgs by navArgs<PluginDetailsFragmentArgs>()
    private val viewModel by viewModel<PluginDetailsViewModel>()
    private val markwon by inject<Markwon>()

    private val glide by lazy {
        Glide.with(requireContext())
    }

    private val screenshotAdapter by lazy {
        PluginDetailsAdapter(
            binding.pluginDetailsScreenshots, emptyList(), viewModel::onScreenshotClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScroll()
        setupMonet()
        setupState()
        setupScreenshots()
        viewModel.setupWithPlugin(navArgs.plugin as Plugin.Remote)
    }

    private fun setupScroll() = with(binding.root) {
        isNestedScrollingEnabled = false
        applyBottomNavigationInset()
    }

    private fun setupMonet() {
        binding.pluginDetailsLoading.loadingProgress.applyMonet()
        binding.pluginDetailsDownloadProgress.applyMonet()
        binding.pluginDetailsDownloadFullWidthButton.applyMonet()
        binding.pluginDetailsDownloadHalfWidthButton1.applyMonet()
        binding.pluginDetailsDownloadHalfWidthButton2.applyMonet()
        binding.pluginDetailsIncompatibility.applyBackgroundTint(monet)
        binding.pluginDetailsDescription.setLinkTextColor(monet.getAccentColor(requireContext()))
        binding.pluginDetailsDescription.highlightColor = monet.getAccentColor(requireContext())
    }

    private fun setupScreenshots() = with(binding.pluginDetailsScreenshots) {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = screenshotAdapter
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
                binding.pluginDetailsLoading.root.isVisible = true
                binding.pluginDetailsLoaded.isVisible = false
                binding.pluginDetailsError.isVisible = false
            }
            is State.Error -> {
                binding.pluginDetailsLoading.root.isVisible = false
                binding.pluginDetailsLoaded.isVisible = false
                binding.pluginDetailsError.isVisible = true
            }
            is State.Loaded -> {
                if (state.remote !is RemotePluginInfo.UpdateJson) return
                binding.pluginDetailsLoading.root.isVisible = false
                binding.pluginDetailsLoaded.isVisible = true
                binding.pluginDetailsError.isVisible = false
                screenshotAdapter.items = state.remote.screenshots?.toList() ?: emptyList()
                if(state.plugin.recommendedApps.isNotEmpty()) {
                    binding.pluginDetailsRecommended.isVisible = true
                    binding.pluginDetailsRecommended.text =
                        getRecommended(state.plugin.recommendedApps)
                }else{
                    binding.pluginDetailsRecommended.isVisible = false
                }
                setupWithPlugin(state)
                handleViewState(state.viewState)
            }
        }
    }

    private fun handleViewState(viewState: PluginViewState) = with(binding) {
        when(viewState){
            is PluginViewState.Install -> {
                pluginDetailsControls.updateDisplayedChild(1)
                pluginDetailsDownloadFullWidthButton.setText(R.string.plugin_details_install)
                whenResumed {
                    pluginDetailsDownloadFullWidthButton.onClicked().collect {
                        viewModel.onInstallClicked()
                    }
                }
            }
            is PluginViewState.Installed -> {
                when {
                    viewState.upToDate && viewState.launchIntent == null -> {
                        //Nothing to launch or update, full width uninstall button
                        pluginDetailsControls.updateDisplayedChild(1)
                        pluginDetailsDownloadFullWidthButton.setText(R.string.plugin_details_uninstall)
                        whenResumed {
                            pluginDetailsDownloadFullWidthButton.onClicked().collect {
                                viewModel.onUninstallClicked()
                            }
                        }
                    }
                    !viewState.upToDate -> {
                        //Update is available: Update & Uninstall buttons
                        pluginDetailsControls.updateDisplayedChild(0)
                        pluginDetailsDownloadHalfWidthButton1.setText(R.string.plugin_details_update)
                        pluginDetailsDownloadHalfWidthButton2.setText(R.string.plugin_details_uninstall)
                        whenResumed {
                            pluginDetailsDownloadHalfWidthButton1.onClicked().collect {
                                viewModel.onInstallClicked()
                            }
                        }
                        whenResumed {
                            pluginDetailsDownloadHalfWidthButton2.onClicked().collect {
                                viewModel.onUninstallClicked()
                            }
                        }
                    }
                    else -> {
                        //Up to date, but with something to open: Open & Uninstall buttons
                        pluginDetailsControls.updateDisplayedChild(0)
                        pluginDetailsDownloadHalfWidthButton1.setText(R.string.plugin_details_open)
                        pluginDetailsDownloadHalfWidthButton2.setText(R.string.plugin_details_uninstall)
                        whenResumed {
                            pluginDetailsDownloadHalfWidthButton1.onClicked().collect {
                                viewModel.onOpenClicked()
                            }
                        }
                        whenResumed {
                            pluginDetailsDownloadHalfWidthButton2.onClicked().collect {
                                viewModel.onUninstallClicked()
                            }
                        }
                    }
                }
            }
            is PluginViewState.Incompatible -> {
                pluginDetailsControls.updateDisplayedChild(3)
                val warningText = when(val reason = viewState.reason){
                    is IncompatibleReason.IncompatibleSDK -> {
                        getString(
                            R.string.plugin_details_incompatible_incompatible_sdk, reason.required
                        )
                    }
                    is IncompatibleReason.OutdatedSmartspacer -> {
                        getString(R.string.plugin_details_incompatible_outdated)
                    }
                    is IncompatibleReason.MissingFeature -> {
                        getString(
                            R.string.plugin_details_incompatible_missing_feature, reason.feature
                        )
                    }
                    is IncompatibleReason.RequiresApp -> {
                        getString(R.string.plugin_details_incompatible_missing_app)
                    }
                }
                pluginDetailsIncompatibilityWarning.text = warningText
                if(viewState.reason is IncompatibleReason.RequiresApp){
                    whenResumed {
                        pluginDetailsIncompatibility.onClicked().collect {
                            viewModel.onInstallCompanionClicked()
                        }
                    }
                }else{
                    pluginDetailsIncompatibility.setOnClickListener(null)
                }
            }
            is PluginViewState.Downloading -> {
                pluginDetailsControls.updateDisplayedChild(2)
                pluginDetailsDownloadProgress.progress = viewState.downloadState.percentage
                pluginDetailsDownloadPercentage.text = "${viewState.downloadState.percentage}%"
                pluginDetailsDownloadSize.text = viewState.downloadState.progressText
            }
            is PluginViewState.StartInstall -> {
                pluginDetailsControls.updateDisplayedChild(1)
                pluginDetailsDownloadFullWidthButton.setText(R.string.plugin_details_start_install)
                whenResumed {
                    pluginDetailsDownloadFullWidthButton.onClicked().collect {
                        viewModel.onStartInstallClicked()
                    }
                }
            }
        }
    }

    private fun setupWithPlugin(state: State.Loaded) = with(binding) {
        val remote = state.remote as RemotePluginInfo.UpdateJson
        state.viewState
        if(remote.icon != null){
            glide.load(remote.icon).transition(withCrossFade())
                .transform(SystemIconShapeTransformation()).into(pluginDetailsIcon)
        }else{
            val packageName = if(state.viewState is PluginViewState.Installed) {
                state.plugin.packageName
            } else BuildConfig.APPLICATION_ID
            glide.load(PackageIcon(packageName)).transition(withCrossFade())
                .transform(SystemIconShapeTransformation()).into(pluginDetailsIcon)
        }
        pluginDetailsTitle.text = state.plugin.name
        pluginDetailsAuthor.text = state.plugin.author
        pluginDetailsDescription.text = markwon.toMarkdown(state.remote.description)
        pluginDetailsDescription.movementMethod = BetterLinkMovementMethod.getInstance()
        if(!state.remote.changelog.isNullOrEmpty()) {
            if((state.viewState as? PluginViewState.Installed)?.upToDate == false) {
                //Show Changelog before description
                pluginDetailsWhatsNew.isVisible = false
                pluginDetailsChangelog.isVisible = true
                pluginDetailsChangelog.text = markwon.toMarkdown(state.remote.changelog)
                    .formatChangelog()
                pluginDetailsChangelog.movementMethod = BetterLinkMovementMethod.getInstance()
            }else{
                //Show Changelog after description
                pluginDetailsChangelog.isVisible = false
                pluginDetailsWhatsNew.isVisible = true
                pluginDetailsWhatsNew.text = markwon.toMarkdown(state.remote.changelog)
                    .formatChangelog()
                pluginDetailsWhatsNew.movementMethod = BetterLinkMovementMethod.getInstance()
            }
        }else{
            //No Changelog to show
            pluginDetailsWhatsNew.isVisible = false
            pluginDetailsChangelog.isVisible = false
        }
    }

    private fun CharSequence.formatChangelog(): CharSequence {
        return SpannableStringBuilder().apply {
            appendLine(getText(R.string.plugin_repository_changelog))
            append(this@formatChangelog)
        }
    }

    override fun getTitle(): CharSequence {
        return ""
    }

    private fun getRecommended(recommendedApps: List<CharSequence>): CharSequence {
        return SpannableStringBuilder().apply {
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
    }

}
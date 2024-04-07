package com.kieronquinn.app.smartspacer.ui.screens.update

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentUpdateBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.update.UpdateViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import io.noties.markwon.Markwon
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class UpdateFragment: BoundFragment<FragmentUpdateBinding>(FragmentUpdateBinding::inflate), BackAvailable {

    private val viewModel by viewModel<UpdateViewModel>()
    private val args by navArgs<UpdateFragmentArgs>()
    private val markwon by inject<Markwon>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupState()
        setupStartInstall()
        setupGitHubButton()
        setupFabState()
        setupFabClick()
        setupInsets()
        viewModel.setupWithRelease(args.release)
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.updateCard.applyBackgroundTint(monet)
        binding.updateStartInstall.setTextColor(accent)
        binding.updateStartInstall.overrideRippleColor(accent)
        binding.updateProgress.applyMonet()
        binding.updateProgressIndeterminate.applyMonet()
        binding.updateIcon.imageTintList = ColorStateList.valueOf(accent)
        binding.updateDownloadBrowser.setTextColor(accent)
        binding.updateFab.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
    }

    private fun setupInsets() {
        binding.updateCard.applyBottomNavigationMargin()
        binding.updateFab.applyBottomNavigationMargin()
    }

    private fun setupStartInstall() = whenResumed {
        binding.updateStartInstall.onClicked().collect {
            viewModel.startInstall()
        }
    }

    private fun setupGitHubButton() = whenResumed {
        binding.updateDownloadBrowser.onClicked().collect {
            viewModel.onDownloadBrowserClicked()
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

    private fun handleState(state: State){
        when(state){
            is State.Loading -> setupWithLoading()
            is State.Info -> setupWithInfo(state)
            is State.Downloading -> setupWithDownloading(state)
            is State.StartInstall -> setupWithDone()
            is State.Failed -> setupWithFailed()
        }
    }

    private fun setupWithLoading() {
        binding.updateInfo.isVisible = false
        binding.updateProgress.isVisible = false
        binding.updateProgressIndeterminate.isVisible = true
        binding.updateTitle.isVisible = true
        binding.updateIcon.isVisible = false
        binding.updateStartInstall.isVisible = false
        binding.updateTitle.setText(R.string.update_loading)
    }

    private fun setupWithInfo(info: State.Info){
        val release = info.release
        binding.updateInfo.isVisible = true
        binding.updateProgress.isVisible = false
        binding.updateProgressIndeterminate.isVisible = false
        binding.updateTitle.isVisible = false
        binding.updateIcon.isVisible = false
        binding.updateStartInstall.isVisible = false
        binding.updateHeading.text = getString(R.string.update_heading, release.versionName)
        binding.updateSubheading.text = getString(R.string.update_subheading, BuildConfig.VERSION_NAME)
        binding.updateBody.text = markwon.toMarkdown(release.body)
        binding.updateInfo.applyBottomNavigationInset()
        whenResumed {
            binding.updateDownloadBrowser.onClicked().collect {
                viewModel.onDownloadBrowserClicked()
            }
        }
    }

    private fun setupWithDownloading(state: State.Downloading) {
        binding.updateInfo.isVisible = false
        binding.updateProgress.isVisible = true
        binding.updateProgressIndeterminate.isVisible = false
        binding.updateTitle.isVisible = true
        binding.updateIcon.isVisible = false
        binding.updateStartInstall.isVisible = false
        binding.updateProgress.progress = state.downloadState.percentage
        binding.updateTitle.setText(R.string.update_downloader_downloading_title)
    }

    private fun setupWithDone() {
        binding.updateInfo.isVisible = false
        binding.updateProgress.isVisible = false
        binding.updateProgressIndeterminate.isVisible = false
        binding.updateTitle.isVisible = true
        binding.updateIcon.isVisible = true
        binding.updateStartInstall.isVisible = true
        binding.updateTitle.setText(R.string.update_done)
        binding.updateIcon.setImageResource(R.drawable.ic_update_download_done)
    }

    private fun setupWithFailed() {
        binding.updateInfo.isVisible = false
        binding.updateProgress.isVisible = false
        binding.updateProgressIndeterminate.isVisible = false
        binding.updateTitle.isVisible = true
        binding.updateIcon.isVisible = true
        binding.updateStartInstall.isVisible = true
        binding.updateTitle.setText(R.string.update_downloader_downloading_failed)
        binding.updateIcon.setImageResource(R.drawable.ic_error_circle)
    }

    private fun setupFabState() {
        handleFabState(viewModel.showFab.value)
        whenResumed {
            viewModel.showFab.collect {
                handleFabState(it)
            }
        }
    }

    private fun handleFabState(showFab: Boolean){
        binding.updateFab.isVisible = showFab
    }

    private fun setupFabClick() = whenResumed {
        binding.updateFab.onClicked().collect {
            viewModel.startDownload()
        }
    }

}
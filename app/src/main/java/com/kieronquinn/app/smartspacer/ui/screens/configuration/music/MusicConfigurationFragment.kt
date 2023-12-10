package com.kieronquinn.app.smartspacer.ui.screens.configuration.music

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.SwitchSetting
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.music.MusicConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class MusicConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<MusicConfigurationViewModel>()

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        val id = requireActivity().intent.getStringExtra(EXTRA_SMARTSPACER_ID) ?: return
        viewModel.setupWithId(id)
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
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.loadItems(), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> = listOf(
        SwitchSetting(
            showAlbumArt,
            getString(R.string.target_music_setting_show_album_art_title),
            getString(R.string.target_music_setting_show_album_art_content),
            ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_target_music_configuration_album_art
            ),
            onChanged = viewModel::onShowAlbumArtChanged
        ),
        SwitchSetting(
            useDoorbell,
            getString(R.string.target_music_setting_use_doorbell_title),
            getString(R.string.target_music_setting_use_doorbell_content),
            ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_target_music_configuration_use_doorbell
            ),
            enabled = showAlbumArt,
            onChanged = viewModel::onUseDoorbellChanged
        ),
        Setting(
            getString(R.string.target_music_setting_clear_dismissed_title),
            getString(R.string.target_music_setting_clear_dismissed_content),
            ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_target_music_configuration_clear_packages
            ),
            onClick = viewModel::onClearPackagesClicked
        )
    )

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}
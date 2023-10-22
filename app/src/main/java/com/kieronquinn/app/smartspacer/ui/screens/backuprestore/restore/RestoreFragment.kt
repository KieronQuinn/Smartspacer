package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRestoreBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.SwitchSetting
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.RestoreViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreFragment : BoundFragment<FragmentRestoreBinding>(FragmentRestoreBinding::inflate), BackAvailable, HideBottomNavigation {

    private val viewModel by viewModel<RestoreViewModel>()
    private val args by navArgs<RestoreFragmentArgs>()

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupClose()
        setupState()
        setupControls()
        setupRecyclerView()
        viewModel.setupWithUri(args.uri)
    }

    private fun setupMonet() {
        binding.backupRestoreRestoreProgress.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.backupRestoreRestoreClose.setTextColor(accent)
        binding.backupRestoreRestoreClose.overrideRippleColor(accent)
    }

    private fun setupClose() = with(binding.backupRestoreRestoreClose) {
        whenResumed {
            onClicked().collect {
                viewModel.onCloseClicked()
            }
        }
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.restoreRestoreControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.restoreRestoreControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.restoreRestoreControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.restoreRestoreControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

    private fun setupRecyclerView() = with(binding.restoreRestoreRecyclerview) {
        adapter = this@RestoreFragment.adapter
        layoutManager = LinearLayoutManager(context)
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
        when (state) {
            is State.Loading -> {
                binding.backupRestoreRestoreTitle.isVisible = true
                binding.backupRestoreRestoreProgress.isVisible = true
                binding.backupRestoreRestoreProgress.isIndeterminate = true
                binding.backupRestoreRestoreTitle.setText(R.string.restore_loading_backup)
                binding.backupRestoreRestoreDesc.isVisible = false
                binding.backupRestoreRestoreIcon.isVisible = false
                binding.backupRestoreRestoreClose.isVisible = false
                binding.restoreRestoreRecyclerview.isVisible = false
                binding.restoreRestoreControls.isVisible = false
            }
            is State.Failed -> {
                binding.backupRestoreRestoreTitle.isVisible = true
                binding.backupRestoreRestoreDesc.isVisible = true
                binding.backupRestoreRestoreIcon.isVisible = true
                binding.backupRestoreRestoreClose.isVisible = true
                binding.backupRestoreRestoreProgress.isVisible = false
                binding.restoreRestoreRecyclerview.isVisible = false
                binding.restoreRestoreControls.isVisible = false
                binding.backupRestoreRestoreIcon.setImageResource(R.drawable.ic_cross_circle)
                binding.backupRestoreRestoreTitle.setText(R.string.restore_error)
                binding.backupRestoreRestoreDesc.text = getString(state.reason.description)
            }
            is State.Loaded -> {
                binding.backupRestoreRestoreTitle.isVisible = false
                binding.backupRestoreRestoreDesc.isVisible = false
                binding.backupRestoreRestoreIcon.isVisible = false
                binding.backupRestoreRestoreClose.isVisible = false
                binding.backupRestoreRestoreProgress.isVisible = false
                binding.restoreRestoreRecyclerview.isVisible = true
                binding.restoreRestoreControls.isVisible = true
                adapter.items = state.config.toItems()
            }
        }
    }

    private fun RestoreConfig.toItems(): List<BaseSettingsItem> {
        return listOf(
            SwitchSetting(
                shouldRestoreTargets,
                getString(R.string.restore_restore_targets_title),
                if(hasTargets){
                    getString(R.string.restore_restore_targets_content)
                }else{
                    getString(R.string.restore_restore_targets_content_empty)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_targets),
                onChanged = viewModel::onRestoreTargetsChanged,
                enabled = hasTargets
            ),
            SwitchSetting(
                shouldRestoreComplications,
                getString(R.string.restore_restore_complications_title),
                if(hasComplications){
                    getString(R.string.restore_restore_complications_content)
                }else{
                    getString(R.string.restore_restore_complications_content_empty)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_complications),
                onChanged = viewModel::onRestoreComplicationsChanged,
                enabled = hasComplications
            ),
            SwitchSetting(
                shouldRestoreRequirements,
                getString(R.string.restore_restore_requirements_title),
                if(hasRequirements){
                    getString(R.string.restore_restore_requirements_content)
                }else{
                    getString(R.string.restore_restore_requirements_content_empty)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_requirements),
                onChanged = viewModel::onRestoreRequirementsChanged,
                enabled = hasRequirements
            ),
            SwitchSetting(
                shouldRestoreExpandedCustomWidgets,
                getString(R.string.restore_restore_widgets_title),
                if(hasExpandedCustomWidgets){
                    getText(R.string.restore_restore_widgets_content)
                }else{
                    getString(R.string.restore_restore_widgets_content_empty)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_widgets),
                onChanged = viewModel::onRestoreWidgetsChanged,
                enabled = hasExpandedCustomWidgets
            ),
            SwitchSetting(
                shouldRestoreSettings,
                getString(R.string.restore_restore_settings_title),
                if(hasExpandedCustomWidgets){
                    getText(R.string.restore_restore_settings_content)
                }else{
                    getString(R.string.restore_restore_settings_content_empty)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings),
                onChanged = viewModel::onRestoreSettingsChanged,
                enabled = hasSettings
            )
        )
    }

    inner class Adapter: BaseSettingsAdapter(binding.restoreRestoreRecyclerview, emptyList())

}
package com.kieronquinn.app.smartspacer.ui.screens.complications.edit

import android.app.Activity
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.shape.CornerFamily
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentEditBinding
import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesOverflow
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.complications.edit.ComplicationEditViewModel.ComplicationHolder
import com.kieronquinn.app.smartspacer.ui.screens.complications.edit.ComplicationEditViewModel.State
import com.kieronquinn.app.smartspacer.utils.appbar.DragOptionalAppBarLayoutBehaviour.Companion.setDraggable
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.collapsedState
import com.kieronquinn.app.smartspacer.utils.extensions.expandProgress
import com.kieronquinn.app.smartspacer.utils.extensions.getRememberedAppBarCollapsed
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.rememberAppBarCollapsed
import com.kieronquinn.app.smartspacer.utils.extensions.setClassLoaderToPackage
import com.kieronquinn.app.smartspacer.utils.extensions.setShadowEnabled
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ComplicationEditFragment: BoundFragment<FragmentEditBinding>(FragmentEditBinding::inflate), LockCollapsed, BackAvailable, ProvidesOverflow {

    private val viewModel by viewModel<ComplicationEditViewModel>()
    private val args by navArgs<ComplicationEditFragmentArgs>()

    private val contentAdapter by lazy {
        SettingsAdapter()
    }

    private val openExternalResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        viewModel.notifyChangeAfterDelay()
    }

    private val dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, MMM d")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.root.setBackgroundColor(background)
        setupContent()
        setupState()
        setupCard()
        setupMonet()
        setupCollapsedState()
        setupPreview()
        viewModel.setupWithComplication(args.action)
    }

    private fun setupMonet() = with(binding.editContentLoading){
        loadingProgress.applyMonet()
    }

    private fun setupCollapsedState() = whenResumed {
        binding.editAppBar.collapsedState().collect {
            rememberAppBarCollapsed(it)
        }
    }

    private fun setupPreview() = with(binding.editPreview) {
        whenResumed {
            binding.editAppBar.expandProgress().collect {
                root.alpha = maxOf((it - 0.6666f) * 3f, 0f)
            }
        }
        val showShadow = !requireContext().isDarkMode
        targetEditPreviewTitle.setShadowEnabled(showShadow)
        targetEditPreviewSubtitle.setShadowEnabled(showShadow)
        targetEditPreviewIcon.setShadowEnabled(showShadow)
    }

    private fun setupContent() = with(binding.editContentRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = contentAdapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupCard() = with(binding.editCardView) {
        setCardBackgroundColor(monet.getBackgroundColor(context))
        val roundedCornerSize = resources.getDimension(R.dimen.margin_16)
        whenResumed {
            binding.editAppBar.expandProgress().collect {
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, roundedCornerSize * it)
                    .setTopRightCorner(CornerFamily.ROUNDED, roundedCornerSize * it)
                    .build()
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
                binding.editAppBar.setExpanded(false, false)
                binding.editAppBar.setDraggable(false)
                binding.editPreview.root.isVisible = false
                binding.editContentRecyclerview.isVisible = false
                binding.editContentLoading.root.isVisible = true
            }
            is State.Loaded -> {
                binding.editAppBar.setDraggable(true)
                binding.editAppBar.setExpanded(!getRememberedAppBarCollapsed())
                binding.editPreview.root.isVisible = true
                binding.editContentRecyclerview.isVisible = true
                binding.editContentLoading.root.isVisible = false
                contentAdapter.update(state.complication.getItems(), binding.editContentRecyclerview)
                setupPreview(state.complication)
            }
        }
    }

    private fun setupPreview(complication: ComplicationHolder) = with(binding.editPreview) {
        targetEditPreviewTitle.text = DateTimeFormatter.ofPattern(dateFormat).format(LocalDateTime.now())
        targetEditPreviewSubtitle.text = complication.config.label
        targetEditPreviewIcon.setImageIcon(complication.config.icon)
    }

    private fun ComplicationHolder.getItems(): List<BaseSettingsItem> {
        val expandedSettings = if(expandedModeEnabled) {
            arrayOf(
                GenericSettingsItem.Header(
                    getString(R.string.complication_edit_expanded_header)
                ),
                GenericSettingsItem.SwitchSetting(
                    complication.expandedShowWhenLocked,
                    getString(R.string.complication_edit_expanded_show_when_locked_title),
                    getString(R.string.complication_edit_expanded_show_when_locked_content),
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_expanded_close_when_locked),
                    onChanged = viewModel::onExpandedShowWhenLockedChanged
                )
            )
        }else emptyArray()
        val musicAvailable = (nativeLockAvailable && nativeMusicAvailable) || oemLockAvailable
        return mutableListOf(
            GenericSettingsItem.Header(
                getString(R.string.complication_edit_target_header)
            ),
            GenericSettingsItem.SwitchSetting(
                complication.showOnHomeScreen,
                getString(R.string.complication_edit_show_on_home_title),
                if(nativeHomeAvailable || oemHomeAvailable) {
                    getString(R.string.complication_edit_show_on_home_content)
                }else{
                    getString(R.string.complication_edit_show_on_home_content_widget)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_show_on_home),
                onChanged = viewModel::onShowOnHomeChanged
            ),
            GenericSettingsItem.SwitchSetting(
                complication.showOnLockScreen,
                if(nativeLockAvailable || oemLockAvailable) {
                    getString(R.string.complication_edit_show_on_lock_screen_title)
                }else{
                    getString(R.string.complication_edit_show_on_lock_screen_title_widget)
                },
                when {
                    nativeLockAvailable -> {
                        getString(R.string.complication_edit_show_on_lock_screen_content)
                    }
                    oemLockAvailable -> {
                        getString(R.string.complication_edit_show_on_lock_screen_content_oem)
                    }
                    else -> {
                        getString(R.string.complication_edit_show_on_lock_screen_content_widget)
                    }
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_show_on_lockscreen),
                onChanged = viewModel::onShowOnLockChanged
            ),
            GenericSettingsItem.SwitchSetting(
                complication.showOnExpanded,
                getString(R.string.complication_edit_show_on_expanded_title),
                if(expandedModeEnabled){
                    getString(R.string.complication_edit_show_on_expanded_content)
                }else{
                    getString(R.string.complication_edit_show_on_expanded_content_disabled)
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_mode),
                onChanged = viewModel::onShowOnExpandedChanged,
                enabled = expandedModeEnabled
            ),
            GenericSettingsItem.SwitchSetting(
                complication.showOnMusic && musicAvailable,
                getString(R.string.complication_edit_show_on_music_title),
                when {
                    nativeLockAvailable && nativeMusicAvailable -> {
                        getString(R.string.complication_edit_show_on_music_content)
                    }
                    oemLockAvailable -> {
                        getString(R.string.complication_edit_show_on_music_content_oem)
                    }
                    !nativeLockAvailable -> {
                        getString(R.string.complication_edit_show_on_music_content_incompatible)
                    }
                    else -> {
                        getString(R.string.complication_edit_show_on_music_content_incompatible_15)
                    }
                },
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_show_on_music),
                onChanged = viewModel::onShowOnMusicChanged,
                enabled = musicAvailable
            ),
            *expandedSettings,
            GenericSettingsItem.Header(
                getString(R.string.complication_edit_settings_header)
            ),
            GenericSettingsItem.Setting(
                getString(R.string.complication_edit_requirements_title),
                complication.getRequirementsContent(),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_requirements),
                onClick = viewModel::onRequirementsClicked
            )
        ).also {
            if(configInfo != null){
                val title = configInfo.label ?: getString(R.string.target_edit_external_title)
                val subtitle = configInfo.description
                    ?: getString(R.string.target_edit_external_content, providerPackageLabel)
                val icon = configInfo.icon
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit_open_external)
                it.add(GenericSettingsItem.Setting(
                    title, subtitle, icon, onClick = ::onOpenExternalClicked
                ))
            }
        }
    }

    private fun Action.getRequirementsContent(): String {
        return when {
            anyRequirements.isNotEmpty() && allRequirements.isNotEmpty() -> {
                getString(
                    R.string.complication_edit_requirements_content_both,
                    anyRequirements.size,
                    allRequirements.size
                )
            }
            anyRequirements.isNotEmpty() -> {
                resources.getQuantityString(
                    R.plurals.complication_edit_requirements_content_any,
                    anyRequirements.size,
                    anyRequirements.size
                )
            }
            allRequirements.isNotEmpty() -> {
                resources.getQuantityString(
                    R.plurals.complication_edit_requirements_content_all,
                    allRequirements.size,
                    allRequirements.size
                )
            }
            else -> getString(R.string.complication_edit_requirements_content)
        }
    }

    private fun onOpenExternalClicked() {
        val complication = (viewModel.state.value as? State.Loaded)?.complication ?: return
        complication.config.configActivity?.apply {
            setClassLoaderToPackage(requireContext(), complication.complication.packageName)
            putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, complication.complication.id)
            putExtra(SmartspacerConstants.EXTRA_AUTHORITY, complication.complication.authority)
        }?.also {
            try {
                openExternalResult.launch(it)
            }catch (e: Exception) {
                Toast.makeText(
                    requireContext(), R.string.complication_edit_external_error, Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_edit, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.edit_delete -> viewModel.onDeleteClicked()
        }
        return true
    }

    override fun onDestroyView() {
        binding.editContentRecyclerview.adapter = null
        super.onDestroyView()
    }

    inner class SettingsAdapter: BaseSettingsAdapter(binding.editContentRecyclerview, emptyList())

}
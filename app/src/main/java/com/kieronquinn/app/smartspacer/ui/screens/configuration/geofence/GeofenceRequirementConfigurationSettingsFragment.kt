package com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.GeofenceRequirement.GeofenceRequirementData
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.GeofenceRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.name.GeofenceRequirementConfigurationNameBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class GeofenceRequirementConfigurationSettingsFragment: BaseSettingsFragment() {

    companion object {
        private const val RADIUS_MINIMUM = 25f
        private const val RADIUS_MAXIMUM = 500f
        private const val RADIUS_STEP = 25f

        private const val LOITERING_DELAY_MINIMUM = 0f
        private const val LOITERING_DELAY_MAXIMUM = 1_800_000f // 30 minutes
        private const val LOITERING_DELAY_STEP = 60_000f // 1 minute

        private const val NOTIFICATION_RESPONSIVENESS_MINIMUM = 0f
        private const val NOTIFICATION_RESPONSIVENESS_MAXIMUM = 300_000f // 5 minutes
        private const val NOTIFICATION_RESPONSIVENESS_STEP = 30_000f // 30 seconds

        private const val ONE_MINUTE = 60_000f
    }

    override val adapter by lazy {
        Adapter(emptyList())
    }

    private val viewModel by lazy {
        requireParentFragment().viewModel<GeofenceRequirementConfigurationViewModel>().value
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMonet()
        setupState()
        setupNameResult()
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
    }

    private fun setupRecyclerView() = with(binding.settingsBaseRecyclerView) {
        binding.settingsBaseRecyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        whenResumed {
            viewModel.settingsTopPadding.collect {
                updatePadding(top = it.toInt())
            }
        }
    }

    private fun setupMonet() {
        binding.settingsBaseLoadingProgress.applyMonet()
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
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.data.toItems(), binding.settingsBaseRecyclerView)
            }
            else -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = false
            }
        }
    }

    private fun setupNameResult() {
        childFragmentManager.setFragmentResultListener(
            GeofenceRequirementConfigurationNameBottomSheetFragment.REQUEST_EDIT_NAME,
            this
        ) { _, result ->
            val name = result.getString(
                GeofenceRequirementConfigurationNameBottomSheetFragment.KEY_NAME
            ) ?: return@setFragmentResultListener
            viewModel.onNameChanged(name)
        }
    }

    private fun onNameClicked() {
        val name = (viewModel.state.value as? State.Loaded)?.data?.name ?: return
        GeofenceRequirementConfigurationNameBottomSheetFragment.newInstance(name)
            .show(
                childFragmentManager,
                GeofenceRequirementConfigurationNameBottomSheetFragment.REQUEST_EDIT_NAME
            )
    }

    private fun GeofenceRequirementData.toItems(): List<BaseSettingsItem> {
        return listOf(
            GenericSettingsItem.Setting(
                getString(R.string.requirement_geofence_configuration_setting_name_title),
                name.ifEmpty {
                    getString(R.string.requirement_geofence_configuration_setting_name_content)
                },
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_requirement_geofence_configuration_name
                ),
                onClick = ::onNameClicked
            ),
            GenericSettingsItem.Slider(
                startValue = radius,
                minValue = RADIUS_MINIMUM,
                maxValue = RADIUS_MAXIMUM,
                step = RADIUS_STEP,
                getString(R.string.requirement_geofence_configuration_setting_radius_title),
                getString(R.string.requirement_geofence_configuration_setting_radius_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_requirement_geofence_configuration_radius
                ),
                {
                   getString(
                       R.string.requirement_geofence_configuration_setting_radius_label,
                       it.roundToInt()
                   )
                },
                viewModel::onRadiusChanged
            ),
            GenericSettingsItem.Slider(
                startValue = loiteringDelay.toFloat(),
                minValue = LOITERING_DELAY_MINIMUM,
                maxValue = LOITERING_DELAY_MAXIMUM,
                step = LOITERING_DELAY_STEP,
                getString(R.string.requirement_geofence_configuration_loitering_delay_title),
                getString(R.string.requirement_geofence_configuration_loitering_delay_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_requirement_geofence_configuration_delay
                ),
                {
                    when (val time = (it / ONE_MINUTE).roundToInt()) {
                        0 -> {
                            getString(
                                R.string.requirement_geofence_configuration_loitering_delay_label_none,
                            )
                        }
                        1 -> {
                            getString(
                                R.string.requirement_geofence_configuration_loitering_delay_label_one,
                                time
                            )
                        }
                        else -> {
                            getString(
                                R.string.requirement_geofence_configuration_loitering_delay_label_many,
                                time
                            )
                        }
                    }
                },
                viewModel::onLoiteringDelayChanged
            ),
            GenericSettingsItem.Slider(
                startValue = notificationResponsiveness.toFloat(),
                minValue = NOTIFICATION_RESPONSIVENESS_MINIMUM,
                maxValue = NOTIFICATION_RESPONSIVENESS_MAXIMUM,
                step = NOTIFICATION_RESPONSIVENESS_STEP,
                getString(R.string.requirement_geofence_configuration_notification_responsiveness_title),
                getString(R.string.requirement_geofence_configuration_notification_responsiveness_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_requirement_geofence_configuration_responsiveness
                ),
                {
                    val time = (it / 1000f).roundToInt()
                    getString(
                        R.string.requirement_geofence_configuration_notification_responsiveness_label,
                        time
                    )
                },
                viewModel::onNotificationResponsivenessChanged
            )
        )
    }

    inner class Adapter(
        override var items: List<BaseSettingsItem>
    ): BaseSettingsAdapter(binding.settingsBaseRecyclerView, items)

}
package com.kieronquinn.app.smartspacer.ui.screens.configuration.date

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DateComplication
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateComplicationConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DateComplicationConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        const val REQUEST_KEY_DATE_FORMAT = "date_format"
        const val RESULT_KEY_DATE_FORMAT = "date_format"
    }

    private val viewModel by viewModel<DateComplicationConfigurationViewModel>()

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupListener()
        viewModel.setup(requireActivity().intent.getStringExtra(EXTRA_SMARTSPACER_ID)!!)
    }

    private fun setupListener() {
        setFragmentResultListener(REQUEST_KEY_DATE_FORMAT) { _, result ->
            val format = result.getString(RESULT_KEY_DATE_FORMAT, "")
            viewModel.onDateFormatChanged(format.takeIf { it.isNotEmpty() })
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
                settingsBaseLoading.isVisible = true
                settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                settingsBaseLoading.isVisible = false
                settingsBaseRecyclerView.isVisible = true
                adapter.update(state.loadItems(), settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return listOf(
            GenericSettingsItem.Setting(
                getString(R.string.target_date_settings_date_format_title),
                data.getDateFormat(),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_target_date),
                onClick = viewModel::onDateFormatClicked
            )
        )
    }

    private fun DateComplication.ComplicationData.getDateFormat(): String {
        val dateFormat = if(dateFormat != null) {
            try {
                DateTimeFormatter.ofPattern(dateFormat)
            }catch (e: IllegalArgumentException) {
                null
            }
        }else null
        return dateFormat?.format(ZonedDateTime.now())
            ?: getString(R.string.target_date_settings_date_format_automatic)
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}
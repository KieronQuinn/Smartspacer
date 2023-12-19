package com.kieronquinn.app.smartspacer.ui.screens.configuration.date.picker

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationTargetDateFormatPickerBottomSheetBinding
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateTargetConfigurationFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DateTargetFormatPickerFragment: BaseBottomSheetFragment<FragmentConfigurationTargetDateFormatPickerBottomSheetBinding>(FragmentConfigurationTargetDateFormatPickerBottomSheetBinding::inflate) {

    companion object {
        private val DATE_FORMATS = arrayOf(
            null,
            "EEE, MMM d",
            "EEEE, MMM d",
            "EEE, d MMM",
            "EEEE, d MMM",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "dd.MM.yyyy",
            "MM.dd.yyyy"
        )
    }

    private val viewModel by viewModel<DateTargetFormatPickerViewModel>()
    private val args by navArgs<DateTargetFormatPickerFragmentArgs>()

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupNegative()
        setupNeutral()
        setupInsets()
    }

    private fun setupRecyclerView() = with(binding.dateFormatRecyclerView) {
        adapter = this@DateTargetFormatPickerFragment.adapter
        layoutManager = LinearLayoutManager(context)
        val items = DATE_FORMATS.map {
            Setting(
                it?.getDate() ?: getString(R.string.target_date_settings_date_format_automatic),
                subtitle = it ?: getString(R.string.target_date_settings_date_format_automatic_content),
                icon = null
            ) {
                onItemClicked(it)
            }
        }
        this@DateTargetFormatPickerFragment.adapter.update(items, this)
    }

    private fun onItemClicked(format: String?) {
        setFragmentResult(
            DateTargetConfigurationFragment.REQUEST_KEY_DATE_FORMAT,
            bundleOf(DateTargetConfigurationFragment.RESULT_KEY_DATE_FORMAT to (format ?: ""))
        )
        dismiss()
    }

    private fun String.getDate(): String? {
        val dateFormat = try {
            DateTimeFormatter.ofPattern(this)
        }catch (e: IllegalArgumentException) {
            null
        }
        return dateFormat?.format(ZonedDateTime.now())
    }

    private fun setupNegative() = with(binding.dateFormatNegative) {
        setTextColor(monet.getAccentColor(requireContext()))
        whenResumed {
            onClicked().collect {
                dismiss()
            }
        }
    }

    private fun setupNeutral() = with(binding.dateFormatNeutral) {
        setTextColor(monet.getAccentColor(requireContext()))
        whenResumed {
            onClicked().collect {
                viewModel.onCustomClicked(args.format ?: "")
            }
        }
    }

    private fun setupInsets() = with(binding.root) {
        val padding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            updatePadding(bottom = bottomInset + padding)
        }
    }

    inner class Adapter: BaseSettingsAdapter(binding.dateFormatRecyclerView, emptyList())

}
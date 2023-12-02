package com.kieronquinn.app.smartspacer.ui.screens.expanded.options

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentExpandedBottomSheetWidgetOptionsBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.expanded.options.ExpandedWidgetOptionsBottomSheetViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExpandedWidgetOptionsBottomSheetFragment: BaseBottomSheetFragment<FragmentExpandedBottomSheetWidgetOptionsBinding>(FragmentExpandedBottomSheetWidgetOptionsBinding::inflate) {

    private val viewModel by viewModel<ExpandedWidgetOptionsBottomSheetViewModel>()
    private val args by navArgs<ExpandedWidgetOptionsBottomSheetFragmentArgs>()

    private val configureLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        //No-op
    }

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoading()
        setupRecyclerView()
        setupInsets()
        setupClose()
        setupState()
        viewModel.setup(args.appWidgetId, args.canReconfigure)
    }

    private fun setupLoading() = with(binding.widgetOptionsLoading.loadingProgress){
        applyMonet()
    }

    private fun setupRecyclerView() = with(binding.widgetOptionsRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@ExpandedWidgetOptionsBottomSheetFragment.adapter
    }

    private fun setupClose() = with(binding.widgetOptionsClose) {
        setTextColor(monet.getAccentColor(requireContext()))
        whenResumed {
            onClicked().collect {
                dismiss()
            }
        }
        whenResumed {
            viewModel.exitBus.collect {
                if(it) {
                    dismiss()
                }
            }
        }
    }

    private fun setupInsets() {
        binding.root.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
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
                binding.widgetOptionsLoading.root.isVisible = true
                binding.widgetOptionsRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.widgetOptionsLoading.root.isVisible = false
                binding.widgetOptionsRecyclerView.isVisible = true
                adapter.update(state.loadItems(), binding.widgetOptionsRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return listOfNotNull(
            GenericSettingsItem.Slider(
                spanX.toFloat(),
                1f,
                5f,
                1f,
                getString(R.string.expanded_custom_widget_options_span_x_title),
                getString(R.string.expanded_custom_widget_options_span_x_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_expanded_custom_widget_options_span_x
                ),
                ::formatLabel,
                viewModel::setSpanX
            ),
            GenericSettingsItem.Slider(
                spanY.toFloat(),
                1f,
                5f,
                1f,
                getString(R.string.expanded_custom_widget_options_span_y_title),
                getString(R.string.expanded_custom_widget_options_span_y_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_expanded_custom_widget_options_span_y
                ),
                ::formatLabel,
                viewModel::setSpanY
            ),
            GenericSettingsItem.Setting(
                getString(R.string.expanded_custom_widget_options_reconfigure_title),
                getString(R.string.expanded_custom_widget_options_reconfigure_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_configure
                )
            ) {
                viewModel.onReconfigureClicked(configureLauncher)
                dismiss()
            }.takeIf { canReconfigure },
            GenericSettingsItem.SwitchSetting(
                showWhenLocked,
                getString(R.string.expanded_custom_widget_options_show_when_locked_title),
                getString(R.string.expanded_custom_widget_options_show_when_locked_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_edit_show_on_lockscreen
                ),
                onChanged = viewModel::setShowWhenLocked
            )
        )
    }

    private fun formatLabel(value: Float): String {
        return value.toInt().toString()
    }

    inner class Adapter: BaseSettingsAdapter(binding.widgetOptionsRecyclerView, emptyList())

}
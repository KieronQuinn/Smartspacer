package com.kieronquinn.app.smartspacer.ui.screens.configuration.widgettarget.setup

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetFragment
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.Item.Widget
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WidgetTargetSetupFragment: ExpandedAddWidgetBottomSheetFragment() {

    override val titleRes = R.string.target_widget_setup_title

    private val viewModel by viewModel<WidgetTargetSetupViewModel> {
        parametersOf(requireActivity().intent.getStringExtra(EXTRA_SMARTSPACER_ID))
    }

    override fun onWidgetClicked(
        item: Widget,
        spanX: Int,
        spanY: Int
    ) {
        viewModel.onWidgetClicked(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }

    private fun setupDismiss() = whenResumed {
        viewModel.dismissBus.collect {
            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()
        }
    }

}
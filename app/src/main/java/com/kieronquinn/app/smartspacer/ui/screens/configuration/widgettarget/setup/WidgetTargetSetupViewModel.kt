package com.kieronquinn.app.smartspacer.ui.screens.configuration.widgettarget.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.smartspace.targets.WidgetTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.Item.Widget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

abstract class WidgetTargetSetupViewModel: ViewModel() {

    abstract val dismissBus: Flow<Unit>

    abstract fun onWidgetClicked(widget: Widget)

}

class WidgetTargetSetupViewModelImpl(
    private val dataRepository: DataRepository,
    private val smartspacerId: String
): WidgetTargetSetupViewModel() {

    override val dismissBus = MutableSharedFlow<Unit>()

    override fun onWidgetClicked(widget: Widget) {
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.WIDGET,
            ::onDataAdded
        ) {
            TargetData(
                widget.info.provider.flattenToString(),
                widget.label.toString(),
                widget.parent.label.toString()
            )
        }
    }

    private fun onDataAdded(context: Context, smartspacerId: String) {
        viewModelScope.launch {
            dismissBus.emit(Unit)
        }
    }

}
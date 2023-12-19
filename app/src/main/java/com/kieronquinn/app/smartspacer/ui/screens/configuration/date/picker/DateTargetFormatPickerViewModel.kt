package com.kieronquinn.app.smartspacer.ui.screens.configuration.date.picker

import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class DateTargetFormatPickerViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onCustomClicked(format: String)

}

class DateTargetFormatPickerViewModelImpl(
    private val navigation: ConfigurationNavigation,
    scope: CoroutineScope? = null
): DateTargetFormatPickerViewModel(scope) {

    override fun onCustomClicked(format: String) {
        vmScope.launch {
            navigation.navigate(
                DateTargetFormatPickerFragmentDirections
                    .actionDateTargetFormatPickerFragmentToDateTargetFormatCustomFragment(format)
            )
        }
    }

}
package com.kieronquinn.app.smartspacer.ui.screens.configuration.date.picker

import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class DateFormatPickerViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onCustomClicked(format: String)

}

class DateFormatPickerViewModelImpl(
    private val navigation: ConfigurationNavigation,
    scope: CoroutineScope? = null
): DateFormatPickerViewModel(scope) {

    override fun onCustomClicked(format: String) {
        vmScope.launch {
            navigation.navigate(
                DateFormatPickerFragmentDirections
                    .actionDateTargetFormatPickerFragmentToDateTargetFormatCustomFragment(format)
            )
        }
    }

}
package com.kieronquinn.app.smartspacer.ui.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

abstract class StateViewModel<T>(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<T>

}
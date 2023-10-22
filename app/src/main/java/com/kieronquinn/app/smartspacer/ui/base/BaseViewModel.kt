package com.kieronquinn.app.smartspacer.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

abstract class BaseViewModel(scope: CoroutineScope?): ViewModel() {

    protected val vmScope = scope ?: viewModelScope

}
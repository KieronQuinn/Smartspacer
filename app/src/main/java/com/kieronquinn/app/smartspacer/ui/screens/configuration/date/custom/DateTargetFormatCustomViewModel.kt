package com.kieronquinn.app.smartspacer.ui.screens.configuration.date.custom

import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

abstract class DateTargetFormatCustomViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setup(format: String)
    abstract fun setFormat(format: String)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val format: String,
            val date: String?
        ): State()
    }

}

class DateTargetFormatCustomViewModelImpl(
    scope: CoroutineScope? = null
): DateTargetFormatCustomViewModel(scope) {

    private val format = MutableStateFlow<String?>(null)
    private val customFormat = MutableStateFlow<String?>(null)

    override val state = combine(
        format.filterNotNull(),
        customFormat
    ) { f, c ->
        val format = c ?: f
        val date = if(format.isNotBlank()) {
            format.getDate()
        }else null
        State.Loaded(format, date)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(format: String) {
        vmScope.launch {
            this@DateTargetFormatCustomViewModelImpl.format.emit(format)
        }
    }

    override fun setFormat(format: String) {
        vmScope.launch {
            customFormat.emit(format)
        }
    }

    private fun String.getDate(): String? {
        val dateFormat = try {
            DateTimeFormatter.ofPattern(this)
        }catch (e: IllegalArgumentException) {
            null
        }
        return dateFormat?.format(ZonedDateTime.now())
    }

}
package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository.State
import kotlinx.coroutines.flow.StateFlow

interface AtAGlanceRepository {

    val statesFlow: StateFlow<List<State>>

    fun getStates(): List<State>
    fun setStates(states: List<State>)

    data class State(
        val title: CharSequence,
        val subtitle: CharSequence,
        val icon: Bitmap,
        val iconContentDescription: CharSequence?,
        val clickIntent: Intent? = null,
        val clickPendingIntent: PendingIntent? = null,
        val optionsIntent: Intent? = null
    )

}

class AtAGlanceRepositoryImpl : AtAGlanceRepository {

    private val _statesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<State>>(emptyList())
    override val statesFlow: StateFlow<List<State>> = _statesFlow

    override fun getStates(): List<State> {
        return _statesFlow.value
    }

    override fun setStates(states: List<State>) {
        _statesFlow.value = states
    }

}
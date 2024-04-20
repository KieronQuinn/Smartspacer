package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository.State

interface AtAGlanceRepository {

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

    private var states: List<State> = emptyList()

    override fun getStates(): List<State> {
        return states
    }

    override fun setStates(states: List<State>) {
        this.states = states
    }

}
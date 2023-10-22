package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository.State

interface AtAGlanceRepository {

    fun getState(): State?
    fun setState(state: State?)

    data class State(
        val title: CharSequence,
        val subtitle: CharSequence,
        val icon: Bitmap,
        val clickIntent: Intent? = null,
        val clickPendingIntent: PendingIntent? = null
    )

}

class AtAGlanceRepositoryImpl : AtAGlanceRepository {

    private var state: State? = null

    override fun getState(): State? {
        return state
    }

    override fun setState(state: State?) {
        this.state = state
    }

}
package com.kieronquinn.app.smartspacer.sdksample.plugin.utils

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.DoorbellState
import com.kieronquinn.app.smartspacer.sdksample.R
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.ExampleLocalImageProvider
import com.kieronquinn.app.smartspacer.sdksample.plugin.targets.DoorbellTarget

class Doorbell {

    companion object {
        @JvmStatic
        private val INSTANCE = Doorbell()

        fun getInstance() = INSTANCE
    }

    private var index = 0
    private var state: DoorbellState = DoorbellState.LoadingIndeterminate()

    fun incrementState(context: Context) {
        state = getNextState(context)
        SmartspacerTargetProvider.notifyChange(context, DoorbellTarget::class.java)
    }

    private fun getNextState(context: Context): DoorbellState {
        index++
        if(index > 5) index = 0
        return when(index){
            0 -> DoorbellState.LoadingIndeterminate()
            1 -> DoorbellState.Loading(
                icon = ContextCompat.getDrawable(context, R.drawable.ic_target_doorbell)!!.toBitmap(),
                tint = true
            )
            2 -> DoorbellState.Videocam()
            3 -> DoorbellState.VideocamOff()
            4 -> DoorbellState.ImageBitmap(
                bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.map_commute)
            )
            5 -> DoorbellState.ImageUri(
                200,
                ExampleLocalImageProvider.getUris(100)
            )
            else -> throw RuntimeException("Invalid index $index")
        }
    }

    fun getState() = state

}
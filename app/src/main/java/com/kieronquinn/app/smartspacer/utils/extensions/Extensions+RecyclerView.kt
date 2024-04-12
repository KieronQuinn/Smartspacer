package com.kieronquinn.app.smartspacer.utils.extensions

import androidx.recyclerview.widget.RecyclerView

suspend fun RecyclerView.runAfterAnimationsFinished(block: () -> Unit) {
    if(isAnimating) {
        awaitPost()
        runAfterAnimationsFinished(block)
    }else{
        block()
    }
}
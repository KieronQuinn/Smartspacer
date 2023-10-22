package com.kieronquinn.app.smartspacer.utils.extensions

import android.widget.ViewFlipper

fun ViewFlipper.updateDisplayedChild(displayed: Int) {
    if(displayedChild == displayed) return
    displayedChild = displayed
}
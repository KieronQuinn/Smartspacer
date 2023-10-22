package com.kieronquinn.app.smartspacer.components.blur

import android.view.View
import android.view.Window

class BlurProvider29: BlurProvider() {

    override val minBlurRadius = 0f
    override val maxBlurRadius = 0f

    override fun applyDialogBlur(dialogWindow: Window, appWindow: Window, ratio: Float) {
        applyBlurToWindow(dialogWindow, ratio)
    }

    override fun applyBlurToWindow(window: Window, ratio: Float) {
        window.addDimming()
    }

    override fun applyBlurToView(view: View, ratio: Float): Boolean {
        return false //Unsupported
    }

}
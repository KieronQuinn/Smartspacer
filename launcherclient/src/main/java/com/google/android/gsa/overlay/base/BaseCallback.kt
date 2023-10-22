package com.google.android.gsa.overlay.base

import android.os.Handler
import android.os.Message
import java.io.PrintWriter

open class BaseCallback : Handler.Callback {

    override fun handleMessage(message: Message): Boolean {
        return true
    }

    open fun dump(printWriter: PrintWriter, str: String) {
        printWriter.println(str + "BaseCallback: nothing to dump")
    }

}
package com.kieronquinn.app.smartspacer.utils.extensions

import android.view.LayoutInflater
import android.view.LayoutInflaterHidden
import dev.rikka.tools.refine.Refine

fun LayoutInflater.setPrivateFactory(factory: LayoutInflater.Factory2) {
    Refine.unsafeCast<LayoutInflaterHidden>(this).setPrivateFactory(factory)
}
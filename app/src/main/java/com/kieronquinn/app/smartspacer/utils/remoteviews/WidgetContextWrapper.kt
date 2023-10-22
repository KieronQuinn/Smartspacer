package com.kieronquinn.app.smartspacer.utils.remoteviews

import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater

class WidgetContextWrapper(context: Context): ContextWrapper(context) {

    override fun getSystemService(name: String): Any {
        if(name == Context.LAYOUT_INFLATER_SERVICE){
            val layoutInflater = super.getSystemService(name) as LayoutInflater
            return WidgetLayoutInflater(this, layoutInflater)
        }
        return super.getSystemService(name)
    }

}
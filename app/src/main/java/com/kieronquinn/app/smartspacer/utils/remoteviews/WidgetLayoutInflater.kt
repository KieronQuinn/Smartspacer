package com.kieronquinn.app.smartspacer.utils.remoteviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetButton
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetCheckBox
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetChronometer
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetGridView
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetImageView
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetListView
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetRadioButton
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetSwitch
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetTextClock
import com.kieronquinn.app.smartspacer.ui.views.widget.WidgetTextView

//Based on https://medium.com/@ilja.kosynkin/creating-custom-layoutinflater-6bd572c4a82a
class WidgetLayoutInflater(
    context: Context,
    parent: LayoutInflater = from(context)
) : LayoutInflater(parent, context) {

    init {
        factory2 = WrapperFactory(factory2)
    }

    companion object {
        private val androidPrefixes = listOf(
            "android.widget.",
            "android.webkit.",
            "android.app."
        )
    }

    override fun cloneInContext(newContext: Context): LayoutInflater {
        return WidgetLayoutInflater(newContext, this)
    }

    override fun onCreateView(
        name: String?,
        attrs: AttributeSet?
    ): View? {
        for (prefix in androidPrefixes) {
            try {
                return createView(name, prefix, attrs)
            } catch (e: ClassNotFoundException) {
                //No-op
            }
        }
        return super.onCreateView(name, attrs)
    }

    inner class WrapperFactory(private val originalFactory: Factory2?) : Factory2 {
        override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
            return originalFactory?.onCreateView(name, context, attrs)
        }

        override fun onCreateView(
            parent: View?, name: String, context: Context, attrs: AttributeSet
        ): View? {
            return when (name) {
                "Button" -> {
                    WidgetButton(context, attrs)
                }
                "CheckBox" -> {
                    WidgetCheckBox(context, attrs)
                }
                "Chronometer" -> {
                    WidgetChronometer(context, attrs)
                }
                "GridView" -> {
                    WidgetGridView(context, attrs)
                }
                "ListView" -> {
                    WidgetListView(context, attrs)
                }
                "RadioButton" -> {
                    WidgetRadioButton(context, attrs)
                }
                "Switch" -> {
                    WidgetSwitch(context, attrs)
                }
                "TextClock" -> {
                    WidgetTextClock(context, attrs)
                }
                "TextView" -> {
                    WidgetTextView(context, attrs)
                }
                "ImageView" -> {
                    WidgetImageView(context, attrs)
                }
                else -> originalFactory?.onCreateView(parent, name, context, attrs)
            }
        }
    }
}
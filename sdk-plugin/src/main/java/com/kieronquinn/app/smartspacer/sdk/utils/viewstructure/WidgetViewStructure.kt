package com.kieronquinn.app.smartspacer.sdk.utils.viewstructure

import android.appwidget.AppWidgetHostView
import androidx.core.view.children
import android.view.View as AndroidView
import android.view.ViewGroup as AndroidViewGroup
import android.widget.AdapterViewFlipper as AndroidAdapterViewFlipper
import android.widget.AnalogClock as AndroidAnalogClock
import android.widget.Button as AndroidButton
import android.widget.CheckBox as AndroidCheckBox
import android.widget.Chronometer as AndroidChronometer
import android.widget.FrameLayout as AndroidFrameLayout
import android.widget.GridLayout as AndroidGridLayout
import android.widget.GridView as AndroidGridView
import android.widget.ImageButton as AndroidImageButton
import android.widget.ImageView as AndroidImageView
import android.widget.LinearLayout as AndroidLinearLayout
import android.widget.ListView as AndroidListView
import android.widget.ProgressBar as AndroidProgressBar
import android.widget.RadioButton as AndroidRadioButton
import android.widget.RadioGroup as AndroidRadioGroup
import android.widget.RelativeLayout as AndroidRelativeLayout
import android.widget.StackView as AndroidStackView
import android.widget.Switch as AndroidSwitch
import android.widget.TextClock as AndroidTextClock
import android.widget.TextView as AndroidTextView
import android.widget.ViewFlipper as AndroidViewFlipper

abstract class View(private val clazz: Class<out AndroidView>) {
    open var index: Int? = null
    open var id: String? = null
    internal var androidId: Int? = null
    internal var visited = false

    internal fun mapFrom(view: AndroidView): Boolean {
        visited = true
        if(view::class.java != clazz) return false
        androidId = view.id
        return true
    }

    fun getViewIdFromStructureId(id: String): Int? {
        if(this.id == id) return androidId
        if(this is ViewGroup){
            return children.firstNotNullOfOrNull { it.value.getViewIdFromStructureId(id) }
        }
        return null
    }

    fun <T : AndroidView> findViewByStructureId(containerView: AndroidView, id: String): T? {
        val rawId = getViewIdFromStructureId(id) ?: return null
        return containerView.findViewById(rawId)
    }

}

abstract class ViewGroup(private val clazz: Class<out AndroidViewGroup>): View(clazz) {
    private var isUsingCustomIndexes: Boolean? = null

    val children = HashMap<Int, View>()
    
    private fun addChild(view: View) {
        val index = view.index?.let {
            if(isUsingCustomIndexes == false) {
                throw MixedIndexesException()
            }
            isUsingCustomIndexes = true
            view.index
        } ?: run {
            if(isUsingCustomIndexes == true) {
                throw MixedIndexesException()
            }
            isUsingCustomIndexes = false
            val childBasedIndex = children.size
            view.index = childBasedIndex
            childBasedIndex
        }
        children[index] = view
    }
    
    fun adapterViewFlipper(block: AdapterViewFlipper.() -> Unit = {}) {
        addChild(AdapterViewFlipper().also(block))
    }
    
    fun frameLayout(block: RelativeLayout.() -> Unit = {}) {
        addChild(RelativeLayout().also(block))
    }

    fun relativeLayout(block: FrameLayout.() -> Unit = {}) {
        addChild(FrameLayout().also(block))
    }
    
    fun gridLayout(block: GridLayout.() -> Unit = {}) {
        addChild(GridLayout().also(block))
    }
    
    fun gridView(block: GridView.() -> Unit = {}) {
        addChild(GridView().also(block))
    }
    
    fun linearLayout(block: LinearLayout.() -> Unit = {}) {
        addChild(LinearLayout().also(block))
    }
    
    fun listView(block: ListView.() -> Unit = {}) {
        addChild(ListView().also(block))
    }
    
    fun stackView(block: StackView.() -> Unit = {}) {
        addChild(StackView().also(block))
    }
    
    fun viewFlipper(block: ViewFlipper.() -> Unit = {}) {
        addChild(ViewFlipper().also(block))
    }
    
    fun analogClock(block: AnalogClock.() -> Unit = {}) {
        addChild(AnalogClock().also(block))
    }
    
    fun button(block: Button.() -> Unit = {}) {
        addChild(Button().also(block))
    }
    
    fun chronometer(block: Chronometer.() -> Unit = {}) {
        addChild(Chronometer().also(block))
    }
    
    fun imageButton(block: ImageButton.() -> Unit = {}) {
        addChild(ImageButton().also(block))
    }
    
    fun imageView(block: ImageView.() -> Unit = {}) {
        addChild(ImageView().also(block))
    }
    
    fun progressBar(block: ProgressBar.() -> Unit = {}) {
        addChild(ProgressBar().also(block))
    }
    
    fun textClock(block: TextClock.() -> Unit = {}) {
        addChild(TextClock().also(block))
    }
    
    fun textView(block: TextView.() -> Unit = {}) {
        addChild(TextView().also(block))
    }
    
    fun checkBox(block: CheckBox.() -> Unit = {}) {
        addChild(CheckBox().also(block))
    }
    
    fun radioButton(block: RadioButton.() -> Unit = {}) {
        addChild(RadioButton().also(block))
    }
    
    fun radioGroup(block: RadioGroup.() -> Unit = {}) {
        addChild(RadioGroup().also(block))
    }
    
    fun switch(block: Switch.() -> Unit = {}) {
        addChild(Switch().also(block))
    }

    internal open fun map(fromView: AndroidView, indent: Int): Boolean {
        visited = true
        //Check the type of this View matches the level passed
        if (fromView::class.java != clazz) return false
        if (fromView !is AndroidViewGroup) return false
        fromView.children.toList().forEachIndexed { index, view ->
            val mapping = children[index] ?: return@forEachIndexed
            //If the mapping of this View isn't successful, the structure is incorrect
            if (!mapping.mapFrom(view)) return false
            if(mapping is ViewGroup){
                //If the mapping of this ViewGroup isn't successful, the structure is incorrect
                if(!mapping.map(view, indent + 1)) return false
            }
        }
        //Verify all expected children have been visited
        return children.values.all { it.visited }
    }

    class MixedIndexesException: IllegalStateException(
        "You cannot mix custom indexes and automatically generated indexes, either set an index for all children of a ViewGroup or none"
    )
}

class AdapterViewFlipper: ViewGroup(AndroidAdapterViewFlipper::class.java)
class FrameLayout: ViewGroup(AndroidFrameLayout::class.java)
class RelativeLayout: ViewGroup(AndroidRelativeLayout::class.java)
class GridLayout: ViewGroup(AndroidGridLayout::class.java)
class GridView: ViewGroup(AndroidGridView::class.java)
class LinearLayout: ViewGroup(AndroidLinearLayout::class.java)
class ListView: View(AndroidListView::class.java)
class StackView: ViewGroup(AndroidStackView::class.java)
class ViewFlipper: ViewGroup(AndroidViewFlipper::class.java)
class AnalogClock: View(AndroidAnalogClock::class.java)
class Button: View(AndroidButton::class.java)
class Chronometer: View(AndroidChronometer::class.java)
class ImageButton: View(AndroidImageButton::class.java)
class ImageView: View(AndroidImageView::class.java)
class ProgressBar: View(AndroidProgressBar::class.java)
class TextClock: View(AndroidTextClock::class.java)
class TextView: View(AndroidTextView::class.java)
class CheckBox: View(AndroidCheckBox::class.java)
class RadioButton: View(AndroidRadioButton::class.java)
class RadioGroup: View(AndroidRadioGroup::class.java)
class Switch: View(AndroidSwitch::class.java)

private class WidgetViewStructure(
    creator: ViewGroup.() -> Unit
): ViewGroup(AppWidgetHostView::class.java) {

    override var index: Int? = null
        set(value) = throw IllegalStateException("Cannot set index on WidgetViewStructure")

    override var id: String? = null
        set(value) = throw IllegalStateException("Cannot set id on WidgetViewStructure")

    init {
        creator(this)
    }


    fun map(fromView: AndroidView): Boolean {
        return map(fromView, 0)
    }

    override fun map(fromView: AndroidView, indent: Int): Boolean {
        val wrapper = AppWidgetHostView(fromView.context).apply {
            addView(fromView)
        }
        return super.map(wrapper, indent)
    }

}

/**
 *  Map a given [AndroidView] to a created ViewStructure ([creator]). This verifies your defined
 *  structure matches that of [view], and maps IDs from [view]'s Views to the structure,
 *  which allows getting the (usually obfuscated or random) View IDs from your structure-defined
 *  ones.
 *
 *  If mapping was not successful (ie. the structure does not match), null is returned.
 */
fun mapWidgetViewStructure(view: AndroidView, creator: ViewGroup.() -> Unit): ViewGroup? {
    val structure = WidgetViewStructure(creator)
    return structure.takeIf { structure.map(view) }
}
package com.kieronquinn.app.smartspacer.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.appcompat.view.menu.ListMenuItemView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnDetach
import androidx.core.view.setPadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate.BlurMode
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.utils.extensions.findActivity
import com.kieronquinn.app.smartspacer.utils.extensions.getBackgroundForBlur
import com.kieronquinn.app.smartspacer.utils.extensions.setRoundedOutline
import com.kieronquinn.monetcompat.core.MonetCompat
import eightbitlab.com.blurview.BlurTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApi")
class SmartspacerListMenuItemView(
    context: Context,
    attrs: AttributeSet?
): ListMenuItemView(context, attrs) {

    companion object {
        private const val TAG_REPARENTED = "reparented"
    }

    private val scope = MainScope()
    private val monet = MonetCompat.getInstance()
    private var blurJob: Job? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reparentIfNeeded()
    }

    private fun View.findListView(): ListView? {
        return when (val parent = parent) {
            is ListView -> parent.findListView()
            else -> this as? ListView
        }
    }

    @Synchronized
    private fun reparentIfNeeded() {
        val listView = findListView() ?: return
        val parent = listView.parent as? ViewGroup ?: return
        if (listView.tag == TAG_REPARENTED) return
        listView.tag = TAG_REPARENTED
        val activity = context.findActivity() ?: return
        val target = activity.findViewById<BlurTarget>(R.id.blur_target) ?: return
        parent.doOnDetach {
            blurJob?.cancel()
            findListView()?.tag = null
        }
        val delegate = BlurDelegate.get(BlurMode.View(listView, target), scope)
        val corners = context.resources.getDimension(R.dimen.margin_24)
        scope.launch {
            delegate.setBlur(1f)
            delegate.view.run {
                setRoundedOutline(corners)
                clipToOutline = true
                setPadding(0)
            }
            delegate.blurAvailable.collect { available ->
                delegate.view.background = (if (available) {
                    monet.getBackgroundForBlur(context)
                } else {
                    context.getAttrColor(com.google.android.material.R.attr.colorSurface)
                }).toDrawable()
            }
        }
    }

}
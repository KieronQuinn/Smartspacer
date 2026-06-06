package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.res.ColorStateList
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.monetcompat.core.MonetCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun MaterialButtonToggleGroup.selectTab(position: Int) {
    check(getChildAt(position).id)
}

fun MaterialButtonToggleGroup.onSelected(includeReselection: Boolean = false) = callbackFlow {
    var lastCheckedId = checkedButtonId
    val listener = MaterialButtonToggleGroup.OnButtonCheckedListener listener@{ group, id, checked ->
        if (!checked || (lastCheckedId == id && !includeReselection)) {
            return@listener
        }
        lastCheckedId = id
        trySend(group.indexOfChild(group.findViewById(id)))
    }
    addOnButtonCheckedListener(listener)
    awaitClose {
        removeOnButtonCheckedListener(listener)
    }
}

fun MaterialButtonToggleGroup.applyMonet(background: Int? = null) {
    val monet = MonetCompat.getInstance()
    val tabBackground = background ?: monet.getBackgroundColorSecondary(context)
        ?: monet.getBackgroundColor(context)
    val tabSelected = monet.getAccentColor(context)
    val primary = context.getAttrColor(android.R.attr.textColorPrimaryInverse)
    val secondary = context.getAttrColor(android.R.attr.textColorSecondary)
    children.filterIsInstance<MaterialButton>().forEach {
        it.backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(tabSelected, tabBackground)
        )
        it.setTextColor(ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(primary, secondary)
        ))
    }
}
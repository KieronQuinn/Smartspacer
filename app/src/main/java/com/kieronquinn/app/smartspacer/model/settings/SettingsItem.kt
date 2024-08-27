package com.kieronquinn.app.smartspacer.model.settings

import android.graphics.drawable.Drawable
import com.google.android.material.slider.LabelFormatter
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.GenericSettingsItemType

abstract class BaseSettingsItem(val itemType: BaseSettingsItemType) {

    open fun deepEquals(other: Any?) = equals(other)
    open fun getItemId() = itemType.itemIndex.toLong()

}

interface BaseSettingsItemType {
    companion object {
        inline fun <reified E: Enum<E>> findIndex(index: Int): E? {
            return enumValues<E>().firstOrNull {
                val itemIndex = (it as? BaseSettingsItemType)?.itemIndex
                    ?: throw RuntimeException("${E::class.java.simpleName} is not a BaseSettingsItemType")
                index == itemIndex
            }
        }
    }

    fun firstIndex() = GenericSettingsItemType.values().size

    val itemIndex
        get() = run {
            (this as Enum<*>).ordinal + firstIndex()
        }
}

sealed class GenericSettingsItem(val type: GenericSettingsItemType): BaseSettingsItem(type) {

    data class Switch(
        val enabled: Boolean,
        val text: CharSequence,
        val onChanged: (checked: Boolean) -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.SWITCH)

    data class Setting(
        val title: CharSequence,
        val subtitle: CharSequence,
        val icon: Drawable?,
        val isEnabled: Boolean = true,
        val onClick: () -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.SETTING) {
        override fun getItemId() = title.hashCode().toLong()
    }

    data class Dropdown<T>(
        val title: CharSequence,
        val subtitle: CharSequence,
        val icon: Drawable?,
        val setting: T,
        val onSet: (T) -> Unit,
        val options: List<T>,
        val adapter: (T) -> Int
    ): GenericSettingsItem(GenericSettingsItemType.DROPDOWN)

    data class SwitchSetting(
        val checked: Boolean,
        val title: CharSequence,
        val subtitle: CharSequence,
        val icon: Drawable?,
        val enabled: Boolean = true,
        val onChanged: (checked: Boolean) -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.SWITCH_SETTING) {
        override fun getItemId() = title.hashCode().toLong()
    }

    data class Slider(
        val startValue: Float,
        val minValue: Float,
        val maxValue: Float,
        val step: Float,
        val title: CharSequence,
        val subtitle: CharSequence,
        val icon: Drawable?,
        val labelFormatter: LabelFormatter? = null,
        val onChanged: (value: Float) -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.SLIDER) {
        override fun getItemId() = title.hashCode().toLong()
    }

    data class Header(
        val text: CharSequence
    ): GenericSettingsItem(GenericSettingsItemType.HEADER) {
        override fun getItemId() = text.hashCode().toLong()
    }

    data class Card(
        val icon: Drawable?,
        val content: CharSequence,
        val onClick: (() -> Unit)? = null,
        val contentHash: Long? = null
    ): GenericSettingsItem(GenericSettingsItemType.CARD) {
        override fun getItemId() = contentHash ?: content.hashCode().toLong()
    }

    data class Footer(
        val icon: Drawable?,
        val text: CharSequence,
        val linkText: CharSequence? = null,
        val onLinkClicked: () -> Unit = {},
    ): GenericSettingsItem(GenericSettingsItemType.FOOTER) {
        override fun getItemId() = text.hashCode().toLong()
    }

    data class RadioCard(
        val isChecked: Boolean,
        val title: CharSequence,
        val content: CharSequence?,
        val onClick: () -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.RADIO_CARD) {
        override fun getItemId() = title.hashCode().toLong()
    }

    enum class GenericSettingsItemType: BaseSettingsItemType {
        HEADER, SWITCH, SETTING, SWITCH_SETTING, SLIDER, DROPDOWN, CARD, FOOTER, RADIO_CARD;

        //Base ItemType starts at 0, then other types go from there
        override fun firstIndex() = 0
    }

}
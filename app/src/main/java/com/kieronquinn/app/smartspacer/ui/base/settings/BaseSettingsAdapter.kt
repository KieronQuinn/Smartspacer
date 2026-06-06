package com.kieronquinn.app.smartspacer.ui.base.settings

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsCardBinding
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsFooterBinding
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsHeaderBinding
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsRadioCardBinding
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsSliderItemBinding
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsSwitchBinding
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsSwitchItemBinding
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsTextItemBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.GenericSettingsItemType
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.getForegroundForBlur
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.views.applyMonet

abstract class BaseSettingsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    open var items: List<BaseSettingsItem>
): LifecycleAwareRecyclerView.Adapter<BaseSettingsAdapter.ViewHolder>(recyclerView) {

    protected val layoutInflater: LayoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)

    private val roundedFull = recyclerView.context.resources.getDimension(R.dimen.margin_16)
    private val roundedPart = recyclerView.context.resources.getDimension(R.dimen.margin_4)
    private val padding = recyclerView.context.resources.getDimensionPixelSize(R.dimen.margin_16)
    private val paddingShort = recyclerView.context.resources.getDimensionPixelSize(R.dimen.margin_2)
    private val paddingMedium = recyclerView.context.resources.getDimensionPixelSize(R.dimen.margin_8)
    private val paddingLarge = recyclerView.context.resources.getDimensionPixelSize(R.dimen.margin_20)

    protected val monet by lazy {
        MonetCompat.getInstance()
    }

    private val settingBackground by lazy {
        monet.getForegroundForBlur(recyclerView.context)
    }

    init {
        this.setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    final override fun getItemId(position: Int): Long {
        return items[position].getItemId()
    }

    final override fun getItemViewType(position: Int): Int {
        return items[position].itemType.itemIndex
    }

    @CallSuper
    open fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        itemType as GenericSettingsItemType
        return when(itemType){
            GenericSettingsItemType.HEADER -> GenericViewHolder.Header(
                ItemSettingsHeaderBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.SWITCH -> GenericViewHolder.Switch(
                ItemSettingsSwitchBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.SETTING -> GenericViewHolder.Setting(
                ItemSettingsTextItemBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.SWITCH_SETTING -> GenericViewHolder.SwitchSetting(
                ItemSettingsSwitchItemBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.SLIDER -> GenericViewHolder.Slider(
                ItemSettingsSliderItemBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.DROPDOWN -> GenericViewHolder.Dropdown(
                ItemSettingsTextItemBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.CARD -> GenericViewHolder.Card(
                ItemSettingsCardBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.FOOTER -> GenericViewHolder.Footer(
                ItemSettingsFooterBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.RADIO_CARD -> GenericViewHolder.RadioCard(
                ItemSettingsRadioCardBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    @CallSuper
    open fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<GenericSettingsItemType>(viewType)
            ?: throw RuntimeException("Failed to find item with index $viewType")
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return onCreateViewHolder(
            parent,
            getItemType(viewType)
        )
    }

    @CallSuper
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when(holder){
            is GenericViewHolder.Header -> holder.setup(item as GenericSettingsItem.Header)
            is GenericViewHolder.Switch -> holder.setup(item as GenericSettingsItem.Switch)
            is GenericViewHolder.Setting -> holder.setup(item as GenericSettingsItem.Setting)
            is GenericViewHolder.SwitchSetting -> holder.setup(item as GenericSettingsItem.SwitchSetting)
            is GenericViewHolder.Slider -> holder.setup(item as GenericSettingsItem.Slider)
            is GenericViewHolder.Dropdown -> holder.setup(item as GenericSettingsItem.Dropdown<*>)
            is GenericViewHolder.Card -> holder.setup(item as GenericSettingsItem.Card)
            is GenericViewHolder.Footer -> holder.setup(item as GenericSettingsItem.Footer)
            is GenericViewHolder.RadioCard -> holder.setup(item as GenericSettingsItem.RadioCard)
        }
        holder.binding.root.applyShape(position)
    }

    protected fun View.applyShape(index: Int) {
        if (this !is MaterialCardView) return
        val next = items.getOrNull(index + 1)?.itemType
        val previous = items.getOrNull(index - 1)?.itemType
        val shouldRound: (BaseSettingsItemType?) -> Boolean = {
            it == null || shouldRound(it)
        }
        setCardBackgroundColor(settingBackground)
        val roundTop = shouldRound(previous)
        val roundBottom = shouldRound(next)
        val shape = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, if (roundTop) roundedFull else roundedPart)
            .setTopRightCorner(CornerFamily.ROUNDED, if (roundTop) roundedFull else roundedPart)
            .setBottomLeftCorner(CornerFamily.ROUNDED, if (roundBottom) roundedFull else roundedPart)
            .setBottomRightCorner(CornerFamily.ROUNDED, if (roundBottom) roundedFull else roundedPart)
            .build()
        shapeAppearanceModel = shape
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(bottom = paddingShort, left = padding, right = padding)
        }
    }

    @CallSuper
    open fun shouldRound(item: BaseSettingsItemType): Boolean {
        return item == GenericSettingsItemType.HEADER || item == GenericSettingsItemType.FOOTER || item == GenericSettingsItemType.SWITCH
    }

    /**
     *  Updates the list, and scrolls to the top if the size has changed
     */
    fun update(
        newList: List<BaseSettingsItem>,
        recyclerView: LifecycleAwareRecyclerView? = null,
        forceScroll: Boolean = false
    ) {
        val hasChangedSize = items.isNotEmpty() && items.size != newList.size
        notifyDataSetChanged()
        items = newList
        if(hasChangedSize || forceScroll) {
            recyclerView?.scrollToPosition(0)
        }
    }

    private fun GenericViewHolder.Header.setup(item: GenericSettingsItem.Header) = with(binding) {
        itemSettingsHeaderTitle.text = item.text
        itemSettingsHeaderTitle.setTextColor(monet.getAccentColor(root.context))
        val topPadding = when {
            item.text == null -> 0
            item.shortTopPadding -> paddingMedium
            else -> paddingLarge
        }
        val bottomPadding = if (item.text == null) 0 else paddingMedium
        itemSettingsHeaderTitle.updatePadding(top = topPadding, bottom = bottomPadding)
    }

    private fun GenericViewHolder.Switch.setup(item: GenericSettingsItem.Switch) = with(binding) {
        itemSettingsSwitchSwitch.text = item.text
        var isChecked = item.enabled
        itemSettingsSwitchSwitch.isChecked = isChecked
        whenResumed {
            binding.itemSettingsSwitchSwitch.onClicked().collect {
                isChecked = !isChecked
                itemSettingsSwitchSwitch.isChecked = isChecked
                item.onChanged(isChecked)
            }
        }
    }

    private fun GenericViewHolder.Setting.setup(item: GenericSettingsItem.Setting) = with(binding) {
        root.alpha = if(item.isEnabled) 1f else 0.5f
        root.isEnabled = item.isEnabled
        itemSettingsTextTitle.text = item.title
        itemSettingsTextContent.text = item.subtitle
        itemSettingsTextContent.isVisible = item.subtitle.isNotEmpty()
        glide.load(item.icon)
            .placeholder(itemSettingsTextIcon.drawable)
            .into(itemSettingsTextIcon)
        itemSettingsTextSpace.isVisible = item.icon == null
        itemSettingsTextIcon.isVisible = item.icon != null
        whenResumed {
            root.onClicked().collect {
                item.onClickWithView?.invoke(root) ?: item.onClick()
            }
        }
    }

    private fun GenericViewHolder.SwitchSetting.setup(item: GenericSettingsItem.SwitchSetting) = with(binding) {
        root.alpha = if(item.enabled) 1f else 0.5f
        root.isEnabled = item.enabled
        itemSettingsSwitchTitle.text = item.title
        itemSettingsSwitchContent.text = item.subtitle
        itemSettingsSwitchContent.isVisible = item.subtitle.isNotEmpty()
        glide.load(item.icon)
            .placeholder(itemSettingsSwitchIcon.drawable)
            .into(itemSettingsSwitchIcon)
        itemSettingsSwitchSpace.isVisible = item.icon == null
        itemSettingsSwitchIcon.isVisible = item.icon != null
        itemSettingsSwitchSwitch.setOnCheckedChangeListener(null)
        itemSettingsSwitchSwitch.isChecked = item.checked
        itemSettingsSwitchSwitch.isEnabled = item.enabled
        itemSettingsSwitchSwitch.applyMonet()
        whenResumed {
            binding.itemSettingsSwitchSwitch.onChanged().collect {
                if(!item.enabled) return@collect
                item.onChanged(it)
            }
        }
        binding.root.setOnClickListener {
            if(!item.enabled) return@setOnClickListener
            binding.itemSettingsSwitchSwitch.toggle()
        }
    }

    private fun GenericViewHolder.Slider.setup(item: GenericSettingsItem.Slider) = with(binding) {
        itemSettingsSliderTitle.text = item.title
        itemSettingsSliderContent.text = item.subtitle
        itemSettingsSliderContent.isVisible = item.subtitle.isNotEmpty()
        glide.load(item.icon)
            .placeholder(itemSettingsSliderIcon.drawable)
            .into(itemSettingsSliderIcon)
        itemSettingsSliderSlider.applyMonet()
        itemSettingsSliderSlider.valueFrom = item.minValue
        itemSettingsSliderSlider.valueTo = item.maxValue
        itemSettingsSliderSlider.value = item.startValue
        itemSettingsSliderSlider.stepSize = item.step
        itemSettingsSliderSlider.setLabelFormatter(item.labelFormatter)
        whenResumed {
            binding.itemSettingsSliderSlider.onChanged().collect {
                item.onChanged(it)
            }
        }
    }

    private fun <T> GenericViewHolder.Dropdown.setup(
        item: GenericSettingsItem.Dropdown<T>
    ) = with(binding) {
        itemSettingsTextTitle.text = item.title
        itemSettingsTextContent.text = item.subtitle
        itemSettingsTextIcon.isVisible = item.icon != null
        itemSettingsTextSpace.isVisible = item.icon == null
        itemSettingsTextContent.isVisible = item.subtitle.isNotEmpty()
        glide.load(item.icon)
            .placeholder(itemSettingsTextIcon.drawable)
            .into(itemSettingsTextIcon)
        whenResumed {
            root.onClicked().collect {
                it.showDropdown(item)
            }
        }
    }

    private fun GenericViewHolder.Card.setup(item: GenericSettingsItem.Card) = with(binding) {
        itemSettingsCardContent.text = item.content
        glide.load(item.icon)
            .placeholder(itemSettingsCardIcon.drawable)
            .into(itemSettingsCardIcon)
        root.isEnabled = item.onClick != null
        root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(top = item.topPadding ?: paddingMedium)
        }
        whenResumed {
            root.onClicked().collect {
                item.onClick?.invoke()
            }
        }
    }

    private fun GenericViewHolder.Footer.setup(item: GenericSettingsItem.Footer) = with(binding) {
        glide.load(item.icon).into(itemSettingsFooterIcon)
        with(itemSettingsFooterLink) {
            val accent = monet.getAccentColor(context)
            val primary = monet.getPrimaryColor(context)
            background.setTint(primary)
            setTextColor(accent)
            paintFlags = paintFlags or Paint.ANTI_ALIAS_FLAG or Paint.UNDERLINE_TEXT_FLAG
        }
        itemSettingsFooterContent.text = item.text
        itemSettingsFooterLink.text = item.linkText
        itemSettingsFooterLink.isVisible = item.linkText != null
        whenResumed {
            itemSettingsFooterLink.onClicked().collect {
                item.onLinkClicked()
            }
        }
    }

    private fun GenericViewHolder.RadioCard.setup(item: GenericSettingsItem.RadioCard) = with(binding) {
        settingsRadioCardTitle.text = item.title
        settingsRadioCardContent.text = item.content
        settingsRadioCardContent.isVisible = item.content != null
        settingsRadioCardRadio.isChecked = item.isChecked
        settingsRadioCardRadio.applyMonet()
        whenResumed {
            settingsRadioCardRadio.onClicked().collect {
                item.onClick()
            }
        }
        whenResumed {
            root.onClicked().collect {
                item.onClick()
            }
        }
    }

    private fun <T> View.showDropdown(
        item: GenericSettingsItem.Dropdown<T>
    ) {
        val popup = PopupMenu(context, this)
        item.options.forEachIndexed { index, option ->
            popup.menu.add(Menu.NONE, index, Menu.NONE, item.adapter(option))
        }
        popup.setOnMenuItemClickListener {
            item.onSet(item.options[it.itemId])
            popup.dismiss()
            true
        }
        popup.show()
    }

    sealed class GenericViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemSettingsHeaderBinding): GenericViewHolder(binding)
        data class Switch(override val binding: ItemSettingsSwitchBinding): GenericViewHolder(binding)
        data class Setting(override val binding: ItemSettingsTextItemBinding): GenericViewHolder(binding)
        data class SwitchSetting(override val binding: ItemSettingsSwitchItemBinding): GenericViewHolder(binding)
        data class Slider(override val binding: ItemSettingsSliderItemBinding): GenericViewHolder(binding)
        data class Dropdown(override val binding: ItemSettingsTextItemBinding): GenericViewHolder(binding)
        data class Card(override val binding: ItemSettingsCardBinding): GenericViewHolder(binding)
        data class Footer(override val binding: ItemSettingsFooterBinding): GenericViewHolder(binding)
        data class RadioCard(override val binding: ItemSettingsRadioCardBinding): GenericViewHolder(binding)
    }

    abstract class ViewHolder(open val binding: ViewBinding): LifecycleAwareRecyclerView.ViewHolder(binding.root)

}
package com.kieronquinn.app.smartspacer.ui.screens.contributors

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class ContributorsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onLinkClicked(url: String)

    sealed class ContributorsSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class LinkedSetting(
            val title: CharSequence,
            val subtitle: CharSequence,
            @DrawableRes
            val icon: Int,
            val onLinkClicked: (url: String) -> Unit
        ): ContributorsSettingsItem(ItemType.LINKED_SETTING)

        enum class ItemType: BaseSettingsItemType {
            LINKED_SETTING
        }

    }

}

class ContributorsViewModelImpl(
    private val navigation: ContainerNavigation,
    scope: CoroutineScope? = null
): ContributorsViewModel(scope) {

    override fun onLinkClicked(url: String) {
        vmScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }

}
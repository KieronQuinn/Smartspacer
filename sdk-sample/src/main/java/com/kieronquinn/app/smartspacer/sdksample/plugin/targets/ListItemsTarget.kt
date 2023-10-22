package com.kieronquinn.app.smartspacer.sdksample.plugin.targets

import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.R
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Shows a title, subtitle, icon and a list of up to three items from a list, a message if the
 *  list is empty. An icon to signify the source can also be provided.
 */
class ListItemsTarget: SmartspacerTargetProvider() {

    private val targets by lazy {
        listOf(
            TargetTemplate.ListItems(
                context = provideContext(),
                id = "shopping_list_target",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "shopping_list_target"),
                title = Text("Shopping List"),
                subtitle = Text("Your shopping list"),
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_shopping_list)),
                listItems = listOf("Bread", "Milk", "Cheese").map { Text(it) },
                emptyListMessage = Text("List Empty"),
                listIcon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_shopping_list)),
                onClick = TapAction(intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
            ).create()
        ).toMutableList()
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return targets
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        targets.clear()
        notifyChange()
        return true
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            "List Items Target",
            "Shows a title, subtitle, icon and a list of up to three items from a list, " +
                    "or a message if the list is empty. An icon to signify the source can also be " +
                    "provided.",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_shopping_list)
        )
    }

}
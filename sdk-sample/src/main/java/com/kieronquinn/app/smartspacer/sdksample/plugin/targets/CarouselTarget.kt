package com.kieronquinn.app.smartspacer.sdksample.plugin.targets

import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.R
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Target showing a title, subtitle, icon and 4 columns of an example weather forecast
 */
class CarouselTarget: SmartspacerTargetProvider() {

    private val items by lazy {
        listOf(
            CarouselTemplateData.CarouselItem(
                Text("M"),
                Text("24°"),
                Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_weather)),
                TapAction(id = "carousel_mon", Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
            ),
            CarouselTemplateData.CarouselItem(
                Text("T"),
                Text("22°"),
                Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_weather)),
                TapAction(id = "carousel_tue", Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
            ),
            CarouselTemplateData.CarouselItem(
                Text("W"),
                Text("22°"),
                Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_weather)),
                TapAction(id = "carousel_wed", Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
            ),
            CarouselTemplateData.CarouselItem(
                Text("T"),
                Text("23°"),
                Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_weather)),
                TapAction(id = "carousel_thu", Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
            )
        )
    }

    private val targets = listOf(
        TargetTemplate.Carousel(
            id = "carousel_target",
            componentName = ComponentName(BuildConfig.APPLICATION_ID, "carousel_target"),
            title = Text("Carousel Target"),
            subtitle = Text("Shows items in columns"),
            icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_carousel)),
            items = items,
            onClick = null,
            onCarouselClick = null
        ).create()
    ).toMutableList()

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
            "Carousel Target",
            "Shows a title, subtitle, icon and 4 columns of an example weather forecast",
            android.graphics.drawable.Icon.createWithResource(provideContext(), R.drawable.ic_target_carousel)
        )
    }

}
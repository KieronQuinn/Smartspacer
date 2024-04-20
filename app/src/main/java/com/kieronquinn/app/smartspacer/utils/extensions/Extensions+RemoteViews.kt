@file:SuppressLint("BlockedPrivateApi")
package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.RemoteViews.RemoteCollectionItems
import android.widget.RemoteViews.RemoteResponse
import android.widget.RemoteViewsHidden
import android.window.SplashScreen
import androidx.annotation.RequiresApi
import androidx.core.widget.RemoteViewsCompat.setImageViewColorFilter
import androidx.core.widget.RemoteViewsCompat.setImageViewImageTintList
import com.kieronquinn.app.smartspacer.providers.SmartspacerWidgetProxyContentProvider.Companion.createSmartspacerWidgetProxyUri
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.ui.activities.OverlayTrampolineActivity
import dev.rikka.tools.refine.Refine
import java.lang.reflect.Field
import java.util.concurrent.CompletableFuture
import kotlin.Pair
import android.util.Pair as AndroidPair

@Suppress("UNCHECKED_CAST")
@SuppressLint("DiscouragedPrivateApi")
private fun RemoteViews.getActions(): ArrayList<Any>? {
    return RemoteViews::class.java.getDeclaredField("mActions").apply {
        isAccessible = true
    }.get(this) as? ArrayList<Any>
}

@Suppress("UNCHECKED_CAST")
@SuppressLint("DiscouragedPrivateApi")
fun RemoteViews.getActionsIncludingNested(): List<Any> {
    val myActions = getActions()?.toList() ?: emptyList()
    val sizedActions = getSizedRemoteViews().mapNotNull {
        it.getActions()
    }.flatten()
    return myActions + sizedActions
}

private val setRemoteViewsAdapterIntentClass by lazy {
    Class.forName("${RemoteViews::class.java.name}\$SetRemoteViewsAdapterIntent")
}

private val setRemoteCollectionItemListAdapterClass by lazy {
    Class.forName("${RemoteViews::class.java.name}\$SetRemoteCollectionItemListAdapterAction")
}

private val setOnClickResponseClass by lazy {
    Class.forName("${RemoteViews::class.java.name}\$SetOnClickResponse")
}

private val actionClass by lazy {
    Class.forName("${RemoteViews::class.java.name}\$Action")
}

private val reflectionActionClass by lazy {
    Class.forName("${RemoteViews::class.java.name}\$ReflectionAction")
}

private val reflectionActionValueField by lazy {
    reflectionActionClass.getDeclaredField("value", "mValue").apply {
        isAccessible = true
    }
}

private val mApplicationField by lazy {
    RemoteViews::class.java.getField("mApplication")
}

fun Any.isRemoteViewsAdapterIntent(): Boolean {
    return this::class.java == setRemoteViewsAdapterIntentClass
}

fun Any.isRemoteCollectionItemListAdapter(): Boolean {
    return this::class.java == setRemoteCollectionItemListAdapterClass
}

fun Any.isOnClickResponse(): Boolean {
    return this::class.java == setOnClickResponseClass
}

fun Any.getId(): Int {
    return actionClass.getDeclaredField("viewId", "mViewId").apply {
        isAccessible = true
    }.get(this) as Int
}

fun Any.extractAdapterIntent(): Pair<Int, Intent> {
    val intent = setRemoteViewsAdapterIntentClass.getDeclaredField("intent", "mIntent").apply {
        isAccessible = true
    }.get(this) as Intent
    return Pair(getId(), intent)
}

@RequiresApi(Build.VERSION_CODES.S)
fun Any.extractRemoteCollectionItems(): Pair<Int, RemoteCollectionItems>? {
    val items = setRemoteCollectionItemListAdapterClass.declaredFields.firstNotNullOfOrNull {
        when(it.name) {
            "mItems" -> getRemoteCollectItemsLegacy(it)
            "mItemsFuture" -> getRemoteCollectItemsFuture(it)
            else -> null
        }
    } ?: return null
    return Pair(getId(), items)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Any.getRemoteCollectItemsLegacy(field: Field): RemoteCollectionItems {
    return field.let {
        it.isAccessible = true
        it.get(this) as RemoteCollectionItems
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Any.getRemoteCollectItemsFuture(field: Field): RemoteCollectionItems {
    return field.let {
        it.isAccessible = true
        val future = it.get(this) as CompletableFuture<RemoteCollectionItems>
        future.get()
    }
}

fun Any.extractOnClickResponse(): Pair<Int, RemoteResponse> {
    val intent = setOnClickResponseClass.getDeclaredField("mResponse").apply {
        isAccessible = true
    }.get(this) as RemoteResponse
    return Pair(getId(), intent)
}

var Any.reflectionActionValue: Any?
    get() = reflectionActionValueField.get(this)
    set(value) = reflectionActionValueField.set(this, value)

fun RemoteViews.removeActionsForId(id: Int) {
    val actions = getActions() ?: return
    actions.removeAll { it.getId() == id }
    getSizedRemoteViews().forEach {
        it.removeActionsForId(id)
    }
}

fun RemoteViews.replaceUriActionsWithProxy(
    context: Context,
    pluginPackageName: String
): RemoteViews = apply {
    val uriActions = getActionsIncludingNested().filter {
        it::class.java == reflectionActionClass && it.reflectionActionValue is Uri
    }.map {
        Pair(it, it.reflectionActionValue as Uri)
    }
    uriActions.forEach {
        val proxyUri = createSmartspacerWidgetProxyUri(it.second)
        context.grantUriPermission(
            pluginPackageName, proxyUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        it.first.reflectionActionValue = proxyUri
    }
}

fun RemoteViews.getPackageName(): String {
    val application = this::class.java.getDeclaredField("mApplication")
        .get(this) as ApplicationInfo
    return application.packageName
}

@RequiresApi(Build.VERSION_CODES.S)
fun RemoteViews.getRemoteViewsToApply(context: Context, widgetSize: SizeF): RemoteViews? {
    return Refine.unsafeCast<RemoteViewsHidden>(this)
        .getRemoteViewsToApply(context, widgetSize)
}

fun RemoteViews.getBestRemoteViews(context: Context, widgetSize: SizeF): RemoteViews {
    val sized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getRemoteViewsToApply(context, widgetSize)
    } else null
    return sized ?: this
}

@SuppressLint("SoonBlockedPrivateApi", "BlockedPrivateApi")
fun RemoteViews.getSizedRemoteViews(): List<RemoteViews> {
    val landscape = RemoteViews::class.java.getDeclaredField("mLandscape").apply {
        isAccessible = true
    }.get(this) as? RemoteViews
    val portrait = RemoteViews::class.java.getDeclaredField("mPortrait").apply {
        isAccessible = true
    }.get(this) as? RemoteViews
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return listOfNotNull(landscape, portrait)
    val sizedRemoteViews = RemoteViews::class.java.getDeclaredField("mSizedRemoteViews")
        .apply { isAccessible = true }.get(this) as? List<RemoteViews> ?: emptyList()
    return listOfNotNull(landscape, portrait, *sizedRemoteViews.toTypedArray())
}

fun RemoteViews.apply(
    context: Context,
    parent: ViewGroup,
    interactionListener: SmartspaceTargetInteractionListener
): View {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val handler = interactionListener
            .getInteractionHandler(null, interactionListener.shouldTrampolineLaunches())
        Refine.unsafeCast<RemoteViewsHidden>(this).apply(context, parent, handler)
    }else{
        val handler = interactionListener
            .getOnClickHandler(null, interactionListener.shouldTrampolineLaunches())
        Refine.unsafeCast<RemoteViewsHidden>(this).apply(context, parent, handler)
    }
}

fun RemoteViews_trampolinePendingIntent(
    view: View,
    pendingIntent: PendingIntent,
    options: AndroidPair<Intent, ActivityOptions>
): Boolean {
    OverlayTrampolineActivity.trampoline(
        view, view.context, pendingIntent, options.second, options.first
    )
    return true
}

fun RemoteViews_startPendingIntent(
    view: View,
    pendingIntent: PendingIntent,
    options: AndroidPair<Intent, ActivityOptions>
): Boolean {
    //Merge the existing options with our animation ones by combining the bundles and then reforming
    val animationOptions = view.createActivityOptions().apply {
        putAll(options.second.toBundle())
    }
    val overrideOptions = AndroidPair(options.first, ActivityOptions_fromBundle(animationOptions))
    return RemoteViews::class.java.getMethod(
        "startPendingIntent",
        View::class.java,
        PendingIntent::class.java,
        AndroidPair::class.java
    ).invoke(null, view, pendingIntent, overrideOptions) as Boolean
}

private fun View.createActivityOptions(): Bundle {
    return ActivityOptions.makeScaleUpAnimation(
        this,
        0,
        0,
        width,
        height
    ).setSplashStyle().toBundle()
}

private fun ActivityOptions.setSplashStyle() = apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON)
    }
}

fun RemoteResponse.getLaunchOptions(view: View): AndroidPair<Intent, ActivityOptions>{
    return RemoteResponse::class.java.getMethod("getLaunchOptions", View::class.java)
        .invoke(this, view) as AndroidPair<Intent, ActivityOptions>
}

fun RemoteViews.setImageViewImageTintListCompat(
    id: Int,
    colourStateList: ColorStateList
) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setImageViewImageTintList(id, colourStateList)
    }else{
        setImageViewColorFilter(id, colourStateList.defaultColor)
    }
}

private fun Class<*>.getDeclaredField(vararg options: String): Field {
    var lastException = NoSuchFieldException()
    return options.firstNotNullOfOrNull {
        try {
            getDeclaredField(it)
        }catch (e: NoSuchFieldException) {
            lastException = e
            null
        }
    } ?: throw lastException
}
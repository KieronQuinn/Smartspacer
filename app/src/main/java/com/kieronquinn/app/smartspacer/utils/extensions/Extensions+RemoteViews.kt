package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.RemoteViews.RemoteCollectionItems
import android.widget.RemoteViews.RemoteResponse
import android.widget.RemoteViewsHidden
import androidx.annotation.RequiresApi
import com.kieronquinn.app.smartspacer.providers.SmartspacerWidgetProxyContentProvider.Companion.createSmartspacerWidgetProxyUri
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.ui.activities.OverlayTrampolineActivity
import dev.rikka.tools.refine.Refine
import java.lang.reflect.Field
import java.util.concurrent.CompletableFuture
import kotlin.Pair
import android.os.Parcelable as RemoteViewsAction
import android.util.Pair as AndroidPair

@Suppress("UNCHECKED_CAST")
@SuppressLint("DiscouragedPrivateApi")
private fun RemoteViews.getActions(): ArrayList<RemoteViewsAction>? {
    return RemoteViews::class.java.getDeclaredField("mActions").apply {
        isAccessible = true
    }.get(this) as? ArrayList<RemoteViewsAction>
}

@Suppress("UNCHECKED_CAST")
@SuppressLint("DiscouragedPrivateApi")
fun RemoteViews.getActionsIncludingNested(): List<RemoteViewsAction> {
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
    reflectionActionClass.getDeclaredField("value").apply {
        isAccessible = true
    }
}

private val mApplicationField by lazy {
    RemoteViews::class.java.getField("mApplication")
}

var RemoteViews.mApplication
    get() = mApplicationField.get(this) as? ApplicationInfo
    set(value) = mApplicationField.set(this, value)

fun RemoteViewsAction.isRemoteViewsAdapterIntent(): Boolean {
    return this::class.java == setRemoteViewsAdapterIntentClass
}

fun RemoteViewsAction.isRemoteCollectionItemListAdapter(): Boolean {
    return this::class.java == setRemoteCollectionItemListAdapterClass
}

fun RemoteViewsAction.isOnClickResponse(): Boolean {
    return this::class.java == setOnClickResponseClass
}

fun RemoteViewsAction.getId(): Int {
    return actionClass.getDeclaredField("viewId").apply {
        isAccessible = true
    }.get(this) as Int
}

fun RemoteViewsAction.extractAdapterIntent(): Pair<Int, Intent> {
    val intent = setRemoteViewsAdapterIntentClass.getDeclaredField("intent").apply {
        isAccessible = true
    }.get(this) as Intent
    return Pair(getId(), intent)
}

@RequiresApi(Build.VERSION_CODES.S)
fun RemoteViewsAction.extractRemoteCollectionItems(): Pair<Int, RemoteCollectionItems>? {
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
private fun RemoteViewsAction.getRemoteCollectItemsLegacy(field: Field): RemoteCollectionItems {
    return field.let {
        it.isAccessible = true
        it.get(this) as RemoteCollectionItems
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun RemoteViewsAction.getRemoteCollectItemsFuture(field: Field): RemoteCollectionItems {
    return field.let {
        it.isAccessible = true
        val future = it.get(this) as CompletableFuture<RemoteCollectionItems>
        future.get()
    }
}

fun RemoteViewsAction.extractOnClickResponse(): Pair<Int, RemoteResponse> {
    val intent = setOnClickResponseClass.getDeclaredField("mResponse").apply {
        isAccessible = true
    }.get(this) as RemoteResponse
    return Pair(getId(), intent)
}

var RemoteViewsAction.reflectionActionValue: Any?
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
        view.context, pendingIntent, options.second, options.first
    )
    return true
}

fun RemoteViews_startPendingIntent(
    view: View,
    pendingIntent: PendingIntent,
    options: AndroidPair<Intent, ActivityOptions>
): Boolean {
    return RemoteViews::class.java.getMethod(
        "startPendingIntent",
        View::class.java,
        PendingIntent::class.java,
        AndroidPair::class.java
    ).invoke(null, view, pendingIntent, options) as Boolean
}

fun RemoteResponse.getLaunchOptions(view: View): AndroidPair<Intent, ActivityOptions>{
    return RemoteResponse::class.java.getMethod("getLaunchOptions", View::class.java)
        .invoke(this, view) as AndroidPair<Intent, ActivityOptions>
}
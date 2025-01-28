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
import com.kieronquinn.app.smartspacer.receivers.WidgetListClickReceiver
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.model.RemoteOnClickResponse.RemoteResponse.Companion.INTERACTION_TYPE_CHECKED_CHANGE
import com.kieronquinn.app.smartspacer.sdk.utils.copy
import com.kieronquinn.app.smartspacer.ui.activities.OverlayTrampolineActivity
import dev.rikka.tools.refine.Refine
import java.lang.reflect.Field
import java.util.concurrent.CompletableFuture
import kotlin.Pair
import android.util.Pair as AndroidPair

@SuppressLint("DiscouragedPrivateApi")
private val mActions = RemoteViews::class.java.getDeclaredField("mActions").apply {
    isAccessible = true
}

@Suppress("UNCHECKED_CAST")
private var RemoteViews.actions: ArrayList<Any>?
    get() = mActions.get(this) as? ArrayList<Any>
    set(value) = mActions.set(this, value)

@Suppress("UNCHECKED_CAST")
@SuppressLint("DiscouragedPrivateApi")
private fun RemoteViews.getActionsIncludingSized(): List<Any> {
    val myActions = actions?.toList() ?: emptyList()
    val sizedActions = getSizedRemoteViews().mapNotNull {
        it.actions
    }.flatten()
    return myActions + sizedActions
}

@SuppressLint("PrivateApi")
fun RemoteViews.getCollectionCache(): Map<String, RemoteCollectionItems> {
    val collectionCache = RemoteViews::class.java.getDeclaredField("mCollectionCache").apply {
        isAccessible = true
    }.get(this)
    return Class.forName("android.widget.RemoteViews\$RemoteCollectionCache")
        .getDeclaredField("mUriToCollectionMapping").apply {
            isAccessible = true
        }.get(collectionCache) as Map<String, RemoteCollectionItems>
}

fun RemoteViews.getActionsIncludingNested(): List<Any> {
    val actions = getActionsIncludingSized()
    val actionsIncludingNested = ArrayList(actions)
    actions.forEach {
        when(it.javaClass.simpleName) {
            "ViewGroupActionAdd" -> {
                it.getViewGroupActionAddRemoteViews().getActionsIncludingNested().let { actions ->
                    actionsIncludingNested.addAll(actions)
                }
            }
        }
    }
    return actionsIncludingNested.distinct()
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
   return setRemoteCollectionItemListAdapterClass.declaredFields.firstNotNullOfOrNull {
        when(it.name) {
            "mItems" -> getRemoteCollectItemsLegacy(it)?.let { items ->
                Pair(getId(), items)
            }
            "mItemsFuture" -> getRemoteCollectItemsFuture(it)?.let { items ->
                Pair(getId(), items)
            }
            else -> null
        }
    }
}

//Requires 36
@RequiresApi(Build.VERSION_CODES.S)
fun Any.extractRemoteCollectionIntent(): Pair<Int, Intent>? {
   return setRemoteCollectionItemListAdapterClass.declaredFields.firstNotNullOfOrNull {
        when(it.name) {
            "mServiceIntent" -> getRemoteCollectIntent(it)?.let { intent ->
                Pair(getId(), intent)
            }
            else -> null
        }
    }
}

sealed class ExtractedRemoteCollectionItems(open val id: Int) {
    data class Items(
        override val id: Int,
        val items: RemoteCollectionItems
    ): ExtractedRemoteCollectionItems(id)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Any.getRemoteCollectItemsLegacy(field: Field): RemoteCollectionItems? {
    return field.let {
        it.isAccessible = true
        it.get(this) as? RemoteCollectionItems
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Any.getRemoteCollectItemsFuture(field: Field): RemoteCollectionItems? {
    return field.let {
        it.isAccessible = true
        val future = it.get(this) as CompletableFuture<RemoteCollectionItems?>
        future.get()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Any.getRemoteCollectIntent(field: Field): Intent? {
    return field.let {
        it.isAccessible = true
        it.get(this) as? Intent
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
    val actions = actions ?: return
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

/**
 *  Replaces click actions with fill in proxy actions, running via [WidgetListClickReceiver]
 */
fun RemoteViews.replaceClickWithFillIntent(): RemoteViews {
    //Not required on Android < 13 since we disable this requirement
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    actions = actions?.map {
        when(it.javaClass.simpleName) {
            "ViewGroupActionAdd" -> it.replaceClickWithFillIntent()
            "SetOnClickResponse" -> it.convertOnClickResponse()
            "SetOnCheckedChangeResponse" -> it.convertOnCheckedChangeResponse()
            else -> it
        }
    } as ArrayList<Any>?
    return this
}

@SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
private fun Any.replaceClickWithFillIntent() = apply {
    val action = Class.forName("android.widget.RemoteViews\$ViewGroupActionAdd")
    val mNestedViews = action.getDeclaredField("mNestedViews").apply {
        isAccessible = true
    }
    val nestedViews = mNestedViews.get(this) as RemoteViews
    mNestedViews.set(this, nestedViews.replaceClickWithFillIntent())
}

@SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
private fun Any.getViewGroupActionAddRemoteViews(): RemoteViews {
    val action = Class.forName("android.widget.RemoteViews\$ViewGroupActionAdd")
    val mNestedViews = action.getDeclaredField("mNestedViews").apply {
        isAccessible = true
    }
    return mNestedViews.get(this) as RemoteViews
}

@SuppressLint("PrivateApi")
private fun Any.convertOnClickResponse() = apply {
    val action = Class.forName("android.widget.RemoteViews\$SetOnClickResponse")
    val mResponse = action.getDeclaredField("mResponse").apply {
        isAccessible = true
    }
    val remoteResponse = mResponse.get(this) as RemoteResponse
    val newResponse = RemoteResponse
        .fromFillInIntent(WidgetListClickReceiver.getIntent(remoteResponse))
    mResponse.set(this, newResponse)
}

@SuppressLint("PrivateApi")
private fun Any.convertOnCheckedChangeResponse() = apply {
    val action = Class.forName("android.widget.RemoteViews\$SetOnCheckedChangeResponse")
    val mResponse = action.getDeclaredField("mResponse").apply {
        isAccessible = true
    }
    val remoteResponse = mResponse.get(this) as RemoteResponse
    val newResponse = RemoteResponse
        .fromFillInIntent(WidgetListClickReceiver.getIntent(remoteResponse))
        .setInteractionType(INTERACTION_TYPE_CHECKED_CHANGE)
    mResponse.set(this, newResponse)
}

private val INCOMPATIBLE_ACTIONS = setOf(
    "SetRemoteCollectionItemListAdapterAction",
    "SetRemoteViewsAdapterIntent"
)

fun RemoteViews.checkCompatibility(): Boolean {
    return getActionsIncludingNested().none {
        INCOMPATIBLE_ACTIONS.contains(it.javaClass.simpleName)
    }
}

@SuppressLint("SoonBlockedPrivateApi")
private val mIsRoot = RemoteViews::class.java.getDeclaredField("mIsRoot").apply {
    isAccessible = true
}

fun RemoteViews.copyAsRoot(): RemoteViews {
    isRoot = true
    return copy()
}

private var RemoteViews.isRoot: Boolean
    get() = mIsRoot.getBoolean(this)
    set(value) = mIsRoot.setBoolean(this, value)
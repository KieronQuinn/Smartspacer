package com.kieronquinn.app.smartspacer.utils.glide

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.utils.extensions.trim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

@GlideModule
class GlideModule: AppGlideModule(), KoinComponent {

    private val loadScope = MainScope()

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            context,
            Icon::class.java,
            Drawable::class.java,
            null,
            this::loadIcon,
            this::getKeyForIcon
        )
        registry.prepend(
            context,
            PackageIcon::class.java,
            Drawable::class.java,
            context.packageManager,
            this::loadPackageIcon,
            this::getKeyForPackageIcon
        )
        registry.prepend(
            context,
            AppShortcut::class.java,
            Drawable::class.java,
            Unit,
            this::loadAppShortcutIcon,
            this::getKeyForAppShortcut
        )
        registry.prepend(
            context,
            AppWidgetProviderInfo::class.java,
            Drawable::class.java,
            Unit,
            this::loadWidget,
            this::getKeyForWidget
        )
    }

    private fun getKeyForIcon(icon: Icon): ObjectKey {
        return ObjectKey(icon.toString())
    }

    private fun loadIcon(
        context: Context,
        icon: Icon,
        dummy: Any?,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        loadScope.launch(Dispatchers.IO) {
            callback.onDataReady(icon.loadDrawable(context))
        }
    }

    private fun getKeyForPackageIcon(icon: PackageIcon): ObjectKey {
        return ObjectKey(icon.packageName)
    }

    private fun loadPackageIcon(
        context: Context,
        icon: PackageIcon,
        packageManager: PackageManager,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        loadScope.launch(Dispatchers.IO) {
            try {
                callback.onDataReady(packageManager.getApplicationIcon(icon.packageName))
            } catch (e: NameNotFoundException) {
                callback.onLoadFailed(e)
            }
        }
    }

    private fun getKeyForAppShortcut(appShortcut: AppShortcut): ObjectKey {
        return ObjectKey("${appShortcut.packageName}:${appShortcut.shortcutId}")
    }

    private fun loadAppShortcutIcon(
        context: Context,
        appShortcut: AppShortcut,
        dummy: Unit,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        loadScope.launch(Dispatchers.IO) {
            val drawable = when {
                appShortcut.icon.icon != null -> {
                    appShortcut.icon.icon.loadDrawable(context)
                }
                appShortcut.icon.descriptor != null -> {
                    val descriptor = appShortcut.icon.descriptor.fileDescriptor
                    val bitmap = BitmapFactory.decodeFileDescriptor(descriptor)
                        .trim(Color.BLACK)
                    BitmapDrawable(context.resources, bitmap)
                }
                else -> null
            }
            callback.onDataReady(drawable)
        }
    }

    private fun getKeyForWidget(widget: AppWidgetProviderInfo): ObjectKey {
        return ObjectKey(widget.toString())
    }

    private fun loadWidget(
        context: Context,
        widget: AppWidgetProviderInfo,
        dummy: Unit,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        loadScope.launch(Dispatchers.IO) {
            val dpi = context.resources.configuration.densityDpi
            val preview = widget.loadPreviewImage(context, dpi)
            //If a preview is available, use that
            if(preview != null) {
                callback.onDataReady(preview)
                return@launch
            }
            //Otherwise, load the app's icon
            callback.onDataReady(widget.loadIcon(context, dpi))
        }
    }



}
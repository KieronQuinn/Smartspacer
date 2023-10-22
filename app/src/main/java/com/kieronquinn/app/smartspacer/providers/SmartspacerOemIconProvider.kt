package com.kieronquinn.app.smartspacer.providers

import android.content.UriMatcher
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.drawable.toBitmap
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.provider.BaseProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.setTint
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class SmartspacerOemIconProvider: BaseProvider() {

    companion object {

        fun createTargetUri(
            targetId: String,
            actionId: String?,
            surface: UiSurface,
            shouldTint: Boolean
        ): Uri {
            return Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .path(Method.GET_TARGET_ICON.path)
                .appendQueryParameter(ARG_TARGET_ID, targetId)
                .appendQueryParameter(ARG_ACTION_ID, actionId)
                .appendQueryParameter(ARG_SURFACE, surface.name)
                .appendQueryParameter(ARG_SHOULD_TINT, shouldTint.toString())
                .build()
        }

        fun createActionUri(actionId: String, surface: UiSurface, shouldTint: Boolean): Uri {
            return Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .path(Method.GET_ACTION_ICON.path)
                .appendQueryParameter(ARG_ACTION_ID, actionId)
                .appendQueryParameter(ARG_SURFACE, surface.name)
                .appendQueryParameter(ARG_SHOULD_TINT, shouldTint.toString())
                .build()
        }

        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.oemiconprovider"
        private const val ARG_TARGET_ID = "target_id"
        private const val ARG_ACTION_ID = "action_id"
        private const val ARG_SHOULD_TINT = "should_tint"
        private const val ARG_SURFACE = "surface"
    }

    private val oemSmartspaceRepository by inject<OemSmartspacerRepository>()
    private val wallpaperRepository by inject<WallpaperRepository>()
    private val grantRepository by inject<GrantRepository>()

    private val iconSize by lazy {
        provideContext().resources.getDimensionPixelSize(R.dimen.oem_smartspace_icon_size)
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        Method.values().forEach {
            it.addToUriMatcher(this)
        }
    }

    override fun getType(uri: Uri): String {
        return "application/octet-stream"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if(!verifyCaller()) return null
        val method = Method.getMethod(uriMatcher.match(uri)) ?: return null
        val tint = getIconTint(uri) ?: return null
        val shouldTint = getShouldTint(uri)
        val icon = when(method) {
            Method.GET_TARGET_ICON -> getTargetIcon(uri)
            Method.GET_ACTION_ICON -> getActionIcon(uri)
        } ?: return null
        val bitmap = icon.icon.loadDrawable(provideContext())
            ?.toBitmap(iconSize, iconSize)?.setTintIfNeeded(tint, shouldTint)
        val inputStream = ByteArrayInputStream(bitmap?.compress())
        val pipe = ParcelFileDescriptor.createPipe()
        val outputStream = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        inputStream.copyTo(outputStream)
        return pipe[0]
    }

    private fun verifyCaller(): Boolean = runBlocking {
        val callingPackage = callingPackage ?: BuildConfig.APPLICATION_ID
        val grant = grantRepository.getGrantForPackage(callingPackage) ?: return@runBlocking false
        grant.oemSmartspace
    }

    private fun getIconTint(uri: Uri): Int? = runBlocking {
        val surface = getSurface(uri) ?: return@runBlocking null
        val darkText = when(surface) {
            UiSurface.HOMESCREEN -> wallpaperRepository.homescreenWallpaperDarkTextColour
            UiSurface.MEDIA_DATA_MANAGER -> wallpaperRepository.homescreenWallpaperDarkTextColour
            UiSurface.LOCKSCREEN -> wallpaperRepository.lockscreenWallpaperDarkTextColour
        }.firstNotNull()
        if(darkText) Color.BLACK else Color.WHITE
    }

    private fun Bitmap.setTintIfNeeded(colour: Int, shouldTint: Boolean): Bitmap {
        return if (shouldTint) setTint(colour) else this
    }

    private fun getShouldTint(uri: Uri): Boolean {
        return uri.getBooleanQueryParameter(ARG_SHOULD_TINT, true)
    }

    private fun getSurface(uri: Uri): UiSurface? {
        val surfaceName = uri.getQueryParameter(ARG_SURFACE) ?: return null
        return UiSurface.valueOf(surfaceName)
    }

    private fun getSmartspaceTarget(uri: Uri): SmartspaceTarget? {
        val targetId = uri.getQueryParameter(ARG_TARGET_ID) ?: return null
        return oemSmartspaceRepository.getSmartspaceTarget(targetId)
    }

    private fun getSmartspaceAction(uri: Uri): SmartspaceAction? {
        val actionId = uri.getQueryParameter(ARG_ACTION_ID) ?: return null
        return oemSmartspaceRepository.getSmartspaceAction(actionId)
    }

    private fun getTargetIcon(uri: Uri): Icon? {
        val target = getSmartspaceTarget(uri) ?: return null
        val targetIcon = target.templateData?.subtitleItem?.icon ?: target.headerAction?.icon?.let {
            Icon(icon = it, shouldTint = target.featureType != SmartspaceTarget.FEATURE_WEATHER)
        }
        if(targetIcon != null) return targetIcon
        val action = getSmartspaceAction(uri) ?: return null
        return action.subItemInfo?.icon ?: action.icon?.let {
            Icon(icon = it, shouldTint = ComplicationTemplate.shouldTint(action))
        }
    }

    private fun getActionIcon(uri: Uri): Icon? {
        val action = getSmartspaceAction(uri) ?: return null
        return action.subItemInfo?.icon ?: action.icon?.let {
            Icon(icon = it, shouldTint = ComplicationTemplate.shouldTint(action))
        }
    }

    /**
     * Compresses the bitmap to a byte array for serialization.
     */
    private fun Bitmap.compress(): ByteArray? {
        val out = ByteArrayOutputStream(getExpectedBitmapSize())
        return try {
            compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            out.toByteArray()
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Try go guesstimate how much space the icon will take when serialized to avoid unnecessary
     * allocations/copies during the write (4 bytes per pixel).
     */
    private fun Bitmap.getExpectedBitmapSize(): Int {
        return width * height * 4
    }

    enum class Method(val path: String) {
        GET_TARGET_ICON("target"), GET_ACTION_ICON("action");

        companion object {
            fun getMethod(code: Int): Method? {
                return values().firstOrNull { it.ordinal == code }
            }
        }

        fun addToUriMatcher(matcher: UriMatcher) {
            matcher.addURI(AUTHORITY, path, ordinal)
        }
    }

}
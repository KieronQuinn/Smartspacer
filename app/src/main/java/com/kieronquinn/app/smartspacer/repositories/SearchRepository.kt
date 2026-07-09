package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.libraries.assistant.oemsmartspace.shared.OEMSmartspaceSharedConstants.GSA_PACKAGE
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.doodle.Doodle
import com.kieronquinn.app.smartspacer.model.doodle.DoodleImage
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.utils.doodle.DoodleInterceptor
import com.kieronquinn.app.smartspacer.utils.extensions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File

interface SearchRepository {

    val expandedSearchApp: Flow<SearchApp?>

    suspend fun getDoodle(): DoodleImage
    fun getAllSearchApps(): List<SearchApp>

    data class SearchApp(
        val packageName: String,
        val label: CharSequence,
        val icon: Drawable?,
        val shouldTint: Boolean,
        val showLensAndMic: Boolean,
        val launchIntent: Intent
    ) {

        val requiresUnlock = packageName != GSA_PACKAGE

    }

}

class SearchRepositoryImpl(
    private val context: Context,
    packageRepository: PackageRepository,
    settingsRepository: SmartspacerSettingsRepository,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope = MainScope(),
    private val packageManager: PackageManager = context.packageManager
): SearchRepository {

    companion object {
        @VisibleForTesting
        var BASE_URL = "https://www.google.com"

        private const val FILENAME_DOODLE_CACHE = "doodle.json"
    }

    private val interceptedClient by lazy {
        okHttpClient.newBuilder().apply {
            addInterceptor(DoodleInterceptor())
        }.build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(interceptedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val cacheFile = File(context.cacheDir, FILENAME_DOODLE_CACHE)
    private val setting = settingsRepository.expandedSearchPackage
    private val webViewFetcher by lazy { com.kieronquinn.app.smartspacer.utils.doodle.WebViewDoodleFetcher(context) }
    // Gson without exclusion strategy so time_to_live_ms is read when parsing WebView responses.
    private val plainGson = com.google.gson.Gson()

    private val doodleApi by lazy {
        retrofit.create(DoodleApi::class.java)
    }

    override val expandedSearchApp = setting.asFlow().flatMapLatest { packageName ->
        packageRepository.onPackageChanged(scope, packageName).map {
            if(packageManager.isValidIntent(getSearchIntent(packageName))) {
                packageName
            }else{
                getDefaultSearchPackage()
            }
        }.map {
            loadSearchApp(it ?: return@map null)
        }
    }

    override suspend fun getDoodle(): DoodleImage = withContext(Dispatchers.IO) {
        val doodle = getCachedDoodle() ?: loadDoodle()
        doodle?.getDoodleImage(BASE_URL) ?: DoodleImage.DEFAULT
    }

    override fun getAllSearchApps(): List<SearchApp> {
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
        return packageManager.queryIntentActivitiesCompat(searchIntent).map {
            loadSearchApp(it.activityInfo.packageName)
        }.sortedBy {
            it.label.toString().lowercase()
        }
    }

    private fun loadSearchApp(packageName: String): SearchApp {
        val label = packageManager.getApplicationInfo(packageName)
            .loadLabel(packageManager)
        val icon = packageManager.getApplicationIcon(packageName)
        val themedIcon = if(packageName == GSA_PACKAGE){
            ContextCompat.getDrawable(context, R.drawable.ic_search_google)
        }else null
        val shouldShowLensAndMic = packageName == GSA_PACKAGE
        val monochrome = if(icon is AdaptiveIconDrawable) {
            icon.monochromeOrNull()
        }else null
        val foreground = if(icon is AdaptiveIconDrawable){
            icon.foreground
        }else null
        return SearchApp(
            packageName,
            label,
            themedIcon ?: monochrome ?: foreground,
            themedIcon == null && monochrome != null,
            shouldShowLensAndMic,
            getSearchIntent(packageName)
        )
    }

    private suspend fun getCachedDoodle(): Doodle? = withContext(Dispatchers.IO) {
        try {
            if(!cacheFile.exists()) return@withContext null
            val cache = cacheFile.readText()
            val doodle = gson.fromJson(cache, CachedDoodle::class.java)
            if(doodle.hasExpired()) return@withContext null
            doodle.doodle
        }catch (e: Exception){
            null
        }
    }

    private suspend fun loadDoodle(): Doodle? {
        // Try WebView first — it uses Chromium's TLS stack, which Google requires to serve
        // a real doodle response. OkHttp's TLS fingerprint causes Google to return {"ddljson":{}}.
        val webViewRaw = try { webViewFetcher.fetchRawJson() } catch (_: Exception) { null }
        val doodle = if (webViewRaw != null) {
            parseAndFillDoodle(webViewRaw)
        } else {
            // Fallback: direct OkHttp call (works when WebView times out or isn't available).
            withContext(Dispatchers.IO) {
                try { doodleApi.getDoodle().invoke()?.fillDarkImage(doodleApi, BASE_URL) }
                catch (_: Exception) { null }
            }
        }
        if (doodle?.getDoodleImage(BASE_URL) == null) return null
        return doodle.also {
            try {
                val cachedDoodle = CachedDoodle(it, it.getExpiryTime())
                cacheFile.writeText(gson.toJson(cachedDoodle))
            } catch (_: Exception) {}
        }
    }

    /** Parses the raw JSON string (possibly with )]}\' prefix) into a filled Doodle. */
    private suspend fun parseAndFillDoodle(raw: String): Doodle? = withContext(Dispatchers.IO) {
        try {
            val json = if (raw.contains("{")) raw.substring(raw.indexOf("{")) else return@withContext null
            // Use plainGson so time_to_live_ms is read from the response.
            plainGson.fromJson(json, Doodle::class.java)?.fillDarkImage(doodleApi, BASE_URL)
        } catch (_: Exception) { null }
    }

    private fun getDefaultSearchPackage(): String? {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        val defaultResolveInfo = packageManager.queryIntentActivitiesCompat(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        ).firstOrNull()
        if(defaultResolveInfo != null) return defaultResolveInfo.activityInfo.packageName
        val resolveInfo = packageManager.queryIntentActivitiesCompat(intent).firstOrNull()
        return resolveInfo?.activityInfo?.packageName
    }

    data class CachedDoodle(
        @SerializedName("doodle")
        val doodle: Doodle,
        @SerializedName("expires_at")
        val expiresAt: Long
    ) {

        fun hasExpired(): Boolean {
            return expiresAt < System.currentTimeMillis()
        }

    }

}

interface DoodleApi {

    @GET("async/ddljson?async=ntp:1")
    fun getDoodle(): Call<Doodle>

    @GET("{path}")
    fun getDoodle(@Path("path", encoded = true) path: String): Call<Void>

}
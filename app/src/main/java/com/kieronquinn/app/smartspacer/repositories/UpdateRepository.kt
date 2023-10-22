package com.kieronquinn.app.smartspacer.repositories

import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.model.github.GitHubRelease
import com.kieronquinn.app.smartspacer.model.update.Release
import com.kieronquinn.app.smartspacer.repositories.UpdateRepository.Companion.CONTENT_TYPE_APK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface UpdateRepository {

    companion object {
        const val CONTENT_TYPE_APK = "application/vnd.android.package-archive"
    }

    suspend fun getUpdate(currentTag: String = BuildConfig.TAG_NAME): Release?

}

class UpdateRepositoryImpl(
    private val settingsRepository: SmartspacerSettingsRepository
): UpdateRepository {

    companion object {
        @VisibleForTesting
        var BASE_URL = "https://api.github.com/repos/KieronQuinn/Smartspacer/"
    }

    private val gitHubService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubService::class.java)

    override suspend fun getUpdate(currentTag: String): Release? = withContext(Dispatchers.IO) {
        if(!settingsRepository.updateCheckEnabled.get()) return@withContext null
        val releasesResponse = try {
            gitHubService.getReleases().execute()
        }catch (e: Exception) {
            return@withContext null
        }
        if(!releasesResponse.isSuccessful) return@withContext null
        val newestRelease = releasesResponse.body()?.firstOrNull() ?: return@withContext null
        if(newestRelease.tag == null || newestRelease.tag == currentTag) return@withContext null
        //Found a new release
        val versionName = newestRelease.versionName ?: return@withContext null
        val asset = newestRelease.assets?.firstOrNull { it.contentType == CONTENT_TYPE_APK }
            ?: return@withContext null
        val downloadUrl = asset.downloadUrl ?: return@withContext null
        val fileName = asset.fileName ?: return@withContext null
        val gitHubUrl = newestRelease.gitHubUrl ?: return@withContext null
        val body = newestRelease.body ?: return@withContext null
        return@withContext Release(
            newestRelease.tag, versionName, downloadUrl, fileName, gitHubUrl, body
        )
    }

}

interface GitHubService {

    @GET("releases")
    fun getReleases(): Call<Array<GitHubRelease>>

}
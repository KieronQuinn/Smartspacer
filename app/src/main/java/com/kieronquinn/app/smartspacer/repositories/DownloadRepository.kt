package com.kieronquinn.app.smartspacer.repositories

import android.app.DownloadManager
import android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
import android.app.DownloadManager.COLUMN_REASON
import android.app.DownloadManager.COLUMN_STATUS
import android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES
import android.app.DownloadManager.Query
import android.app.DownloadManager.Request
import android.app.DownloadManager.STATUS_FAILED
import android.app.DownloadManager.STATUS_PENDING
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.text.format.Formatter
import androidx.core.content.FileProvider
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import java.io.File
import kotlin.math.roundToInt

interface DownloadRepository {

    fun download(request: DownloadRequest): Flow<DownloadState>
    fun cancelDownload(id: Long)

    sealed class DownloadState(open val id: Long) {
        data class Progress(
            override val id: Long, val percentage: Int, val progressText: String
        ): DownloadState(id)
        data class DownloadComplete(override val id: Long, val file: Uri): DownloadState(id)
        data class Failed(override val id: Long, val reason: Int): DownloadState(id)
    }

    data class DownloadRequest(
        val url: String,
        val title: CharSequence,
        val description: CharSequence,
        val outFileName: String
    )

}

class DownloadRepositoryImpl(private val context: Context): DownloadRepository {

    private val scope = MainScope()

    private val downloadManager = context.getSystemService(
        Context.DOWNLOAD_SERVICE
    ) as DownloadManager

    private val downloadFolder = File(context.externalCacheDir, "downloads").apply {
        mkdirs()
    }

    private val downloadObserver = context.contentResolver.observerAsFlow(
        Uri.parse("content://downloads/my_downloads")
    ).mapLatest {
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    override fun download(request: DownloadRequest) = flow {
        //Clear any old downloads (we don't support asynchronous downloads)
        downloadFolder.deleteRecursively()
        val outFile = File(downloadFolder, request.outFileName)
        val downloadRequest = Request(Uri.parse(request.url)).apply {
            setTitle(request.title)
            setDescription(request.description)
            setDestinationUri(Uri.fromFile(outFile))
        }
        try {
            val id = downloadManager.enqueue(downloadRequest)
            val out = getUriForFile(outFile)
            downloadState(id, out).filterNotNull().collect {
                emit(it)
            }
        }catch (e: Exception){
            //Downloads directory is not accessible for some reason
        }
    }

    private fun downloadState(id: Long, outUri: Uri) = downloadObserver.mapNotNull {
        val query = Query()
        query.setFilterById(id)
        val cursor: Cursor = downloadManager.query(query)
        var progress = 0.0
        var size = 0
        var downloaded = 0
        var status = STATUS_PENDING
        var reason = -1
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(COLUMN_STATUS)
            val reasonIndex = cursor.getColumnIndex(COLUMN_REASON)
            val sizeIndex = cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES)
            val downloadedIndex = cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR)
            status = cursor.getInt(statusIndex)
            reason = cursor.getInt(reasonIndex)
            size = cursor.getInt(sizeIndex)
            downloaded = cursor.getInt(downloadedIndex)
            if (size != -1){
                progress = downloaded * 100.0 / size
            }
        }
        when {
            status == STATUS_FAILED || reason == -1 -> {
                DownloadState.Failed(id, reason)
            }
            status == STATUS_SUCCESSFUL -> {
                DownloadState.DownloadComplete(id, outUri)
            }
            else -> {
                val sizeFormatted = Formatter.formatFileSize(
                    context, size.coerceAtLeast(0).toLong()
                )
                val downloadedFormatted = Formatter.formatFileSize(
                    context, downloaded.coerceAtLeast(0).toLong()
                )
                val caption = if(size >= 0){
                    context.getString(
                        R.string.download_size,
                        downloadedFormatted,
                        sizeFormatted
                    )
                }else ""
                DownloadState.Progress(id, progress.roundToInt(), caption)
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun cancelDownload(id: Long) {
        downloadManager.remove(id)
    }

    private fun getUriForFile(downloadFile: File): Uri {
        return FileProvider.getUriForFile(
            context, BuildConfig.APPLICATION_ID + ".provider", downloadFile
        )
    }

}
package com.kieronquinn.app.smartspacer.model.update

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Release(
    val tag: String,
    val versionName: String,
    val downloadUrl: String,
    val fileName: String,
    val gitHubUrl: String,
    val body: String
): Parcelable
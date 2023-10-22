package com.kieronquinn.app.smartspacer.model.media

import android.app.PendingIntent
import android.media.MediaMetadata

data class MediaContainer(
    val packageName: String,
    val metadata: MediaMetadata?,
    val sessionActivity: PendingIntent?
) {

    companion object {
        private val COMPARE_KEYS = arrayOf(
            MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
            MediaMetadata.METADATA_KEY_TITLE,
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI
        )
    }

    /**
     *  Compares the metadata we actually care about - title, subtitle, icon. The default
     *  implementation of this compares **every key** and thus some apps are very spammy and cause
     *  binder issues with too many updates.
     *
     *  Thanks, TuneIn, for sending EVERY SINGLE PROGRESS UPDATE.
     */
    override fun equals(other: Any?): Boolean {
        if(other !is MediaContainer) return false
        //Package names not matching = session has changed
        if(packageName != other.packageName) return false
        //Session activity changes are tracked
        if(sessionActivity != other.sessionActivity) return false
        //If both metadata are null, it's counted as the same session and no update
        if(metadata == null && other.metadata == null) return true
        //If only one metadata is now null, it's a change but we don't know why
        if(metadata == null || other.metadata == null) return false
        //Check the keys we care about for changes
        COMPARE_KEYS.forEach {
            if(metadata.getString(it) != other.metadata.getString(it)) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + (sessionActivity?.hashCode() ?: 0)
        return result
    }

}

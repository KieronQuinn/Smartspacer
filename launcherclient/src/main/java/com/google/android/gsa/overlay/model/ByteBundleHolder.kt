package com.google.android.gsa.overlay.model

import android.os.Bundle

data class ByteBundleHolder(val bytes: ByteArray, val extras: Bundle) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ByteBundleHolder
        if (!bytes.contentEquals(other.bytes)) return false
        if (extras != other.extras) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + extras.hashCode()
        return result
    }

}
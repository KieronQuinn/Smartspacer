package com.kieronquinn.app.smartspacer.sdk.utils

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import java.io.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("DEPRECATION")
fun <T: Parcelable> Bundle.getParcelableCompat(key: String, type: Class<T>): T? {
    classLoader = type.classLoader
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, type)
    } else {
        getParcelable(key)
    }
}

@Suppress("DEPRECATION")
fun <T: Parcelable> Bundle.getParcelableArrayListCompat(key: String, clazz: Class<T>): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, clazz)
    } else {
        getParcelableArrayList(key)
    }
}

@Suppress("DEPRECATION")
fun <T: Parcelable> Bundle.getParcelableArrayListNullableCompat(key: String, clazz: Class<T>): ArrayList<T?>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, clazz)
    } else {
        getParcelableArrayList(key)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("DEPRECATION")
fun <T: Serializable> Bundle.getSerializableCompat(key: String, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, type)
    } else {
        getSerializable(key) as? T
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun <E : Enum<E>> Bundle.putEnumList(key: String, enums: List<Enum<E>>) {
    putStringArray(key, enums.map { it.name }.toTypedArray())
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
inline fun <reified E : Enum<E>> Bundle.getEnumList(key: String): List<E>? {
    val values = enumValues<E>()
    val keys = getStringArray(key) ?: return null
    return keys.mapNotNull { k -> values.firstOrNull { e -> e.name == k } }
}


package com.kieronquinn.app.smartspacer.utils.extensions

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.kieronquinn.app.smartspacer.sdk.utils.ParceledListSlice

@Suppress("DEPRECATION")
fun <T: Parcelable> Parcel.readParcelableCompat(classLoader: ClassLoader, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelable(classLoader, type)
    } else {
        readParcelable(classLoader)
    }
}

fun List<Parcelable>.getParcelSize(): Int {
    return ParceledListSlice(this).getSize()
}

fun Parcelable.getSize(): Int {
    return Parcel.obtain().let { parcel ->
        writeToParcel(parcel, 0)
        parcel.dataSize().also {
            parcel.recycle()
        }
    }
}
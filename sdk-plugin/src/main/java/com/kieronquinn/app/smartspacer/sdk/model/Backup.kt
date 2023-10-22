package com.kieronquinn.app.smartspacer.sdk.model

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf

/**
 *  Represents a backup of a Target, Complication or Requirement, used either in creating or
 *  restoring a backup for all items in Smartspacer.
 */
data class Backup(
    /**
     *  Serialized data of the backup. Ideally, this should be in a format like JSON. Please
     *  note that since backups are passed through Binder, the total size of [Backup] must not
     *  exceed 1Mb.
     */
    val data: String? = null,
    /**
     *  Optional: Provide a user-friendly name for this backed up item. This will be displayed
     *  to the user during the restore process to inform them what the item was setup to do,
     *  so even if you cannot make a backup then providing this name may help them re-configure
     *  the item.
     */
    val name: String? = null
) : Parcelable {

    companion object CREATOR : Parcelable.Creator<Backup> {
        private const val KEY_DATA = "data"
        private const val KEY_NAME = "name"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        override fun createFromParcel(parcel: Parcel): Backup {
            return Backup(parcel)
        }

        override fun newArray(size: Int): Array<Backup?> {
            return arrayOfNulls(size)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle) : this(
        bundle.getString(KEY_DATA),
        bundle.getString(KEY_NAME)
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle() = bundleOf(
        KEY_DATA to data,
        KEY_NAME to name
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(data)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

}
package com.google.android.gsa.overlay.base

import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.RemoteException

open class BaseInterface(private val binder: IBinder, private val interfaceToken: String) : IInterface {
    override fun asBinder(): IBinder {
        return binder
    }

    fun createParcel(): Parcel {
        val obtain = Parcel.obtain()
        obtain.writeInterfaceToken(interfaceToken)
        return obtain
    }

    fun transactInt(i: Int, parcel: Parcel) {
        try {
            binder.transact(i, parcel, null, 1)
        } catch (e: RemoteException) {
            e.printStackTrace()
        } finally {
            parcel.recycle()
        }
    }
}
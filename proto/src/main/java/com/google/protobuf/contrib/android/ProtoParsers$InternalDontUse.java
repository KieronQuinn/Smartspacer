package com.google.protobuf.contrib.android;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *  Barebones re-implementation of Protobuf's internal parser. Allows loading ByteArray-backed
 *  Protobuf from a Parcel, and then using a custom Proto to parse it. Also supports writing back
 *  to a Parcel.
 */
public final class ProtoParsers$InternalDontUse implements Parcelable {

    public int length;
    public byte[] bytes;

    public ProtoParsers$InternalDontUse(int length, byte[] bytes) {
        this.length = length;
        this.bytes = bytes;
    }

    private ProtoParsers$InternalDontUse(Parcel in) {
        length = in.readInt();
        bytes = in.createByteArray();
    }

    public static final Creator<ProtoParsers$InternalDontUse> CREATOR = new Creator<ProtoParsers$InternalDontUse>() {
        @Override
        public ProtoParsers$InternalDontUse createFromParcel(Parcel in) {
            return new ProtoParsers$InternalDontUse(in);
        }

        @Override
        public ProtoParsers$InternalDontUse[] newArray(int size) {
            return new ProtoParsers$InternalDontUse[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(length);
        parcel.writeByteArray(bytes);
    }

}


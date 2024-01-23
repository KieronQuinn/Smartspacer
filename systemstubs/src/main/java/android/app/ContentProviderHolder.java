/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app;

import android.content.IContentProvider;
import android.content.pm.ProviderInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information you can retrieve about a particular application.
 */
public class ContentProviderHolder implements Parcelable {
    public final ProviderInfo info;
    public IContentProvider provider;
    public IBinder connection;
    public boolean noReleaseNeeded;

    /**
     * Whether the provider here is a local provider or not.
     */
    public boolean mLocal;

    public ContentProviderHolder(ProviderInfo _info) {
        info = _info;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    public static final @androidx.annotation.NonNull Parcelable.Creator<ContentProviderHolder> CREATOR
            = new Parcelable.Creator<ContentProviderHolder>() {
        @Override
        public ContentProviderHolder createFromParcel(Parcel source) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public ContentProviderHolder[] newArray(int size) {
            throw new RuntimeException("Stub!");
        }
    };

    private ContentProviderHolder(Parcel source) {
        throw new RuntimeException("Stub!");
    }
}
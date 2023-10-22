/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.app.prediction;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The id for a prediction target. See {@link AppTarget}.
 */
public final class AppTargetId implements Parcelable {

    /**
     * Creates a new id for a prediction target.
     */
    public AppTargetId(@NonNull String id) {
        throw new RuntimeException("Stub!");
    }

    private AppTargetId(Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the id.
     *
     * @hide
     */
    @NonNull
    public String getId() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    public static final @androidx.annotation.NonNull Creator<AppTargetId> CREATOR =
            new Creator<AppTargetId>() {
                public AppTargetId createFromParcel(Parcel parcel) {
                    return new AppTargetId(parcel);
                }

                public AppTargetId[] newArray(int size) {
                    return new AppTargetId[size];
                }
            };
}
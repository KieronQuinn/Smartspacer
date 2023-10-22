/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.app.smartspace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * The id for an Smartspace session. See {@link SmartspaceSession}.
 *
 */
public final class SmartspaceSessionId implements Parcelable {

    @NonNull
    private final String mId;

    @NonNull
    private final UserHandle mUserHandle;

    /**
     * Creates a new id for a Smartspace session.
     *
     */
    public SmartspaceSessionId(@NonNull final String id, @NonNull final UserHandle userHandle) {
        throw new RuntimeException("Stub!");
    }

    private SmartspaceSessionId(Parcel p) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns a {@link String} Id of this sessionId.
     */
    @Nullable
    public String getId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the userId associated with this sessionId.
     */
    @NonNull
    public UserHandle getUserHandle() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String toString() {
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
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    public static final @NonNull Creator<SmartspaceSessionId> CREATOR =
            new Creator<SmartspaceSessionId>() {
                public SmartspaceSessionId createFromParcel(Parcel parcel) {
                    return new SmartspaceSessionId(parcel);
                }

                public SmartspaceSessionId[] newArray(int size) {
                    return new SmartspaceSessionId[size];
                }
            };
}
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link SmartspaceConfig} instance is supposed to be created by a smartspace client for each
 * UISurface. The client can specify some initialization conditions for the UISurface like its name,
 * expected number of smartspace cards etc. The clients can also specify if they want periodic
 * updates or their desired maximum refresh frequency.
 *
 */
public final class SmartspaceConfig implements Parcelable {

    /**
     * The least number of smartspace targets expected to be predicted by the backend. The backend
     * will always try to satisfy this threshold but it is not guaranteed to always meet it.
     */
    @IntRange(from = 0, to = 50)
    private final int mSmartspaceTargetCount;

    /**
     * A {mUiSurface} is the name of the surface which will be used to display the cards. A
     * few examples are homescreen, lockscreen, aod etc.
     */
    @NonNull
    private final String mUiSurface;

    /** Package name of the client. */
    @NonNull
    private String mPackageName;

    /**
     * Send other client UI configurations in extras.
     *
     * This can include:
     *
     * - Desired maximum update frequency (For example 1 minute update frequency for AoD, 1 second
     * update frequency for home screen etc).
     * - Request to get periodic updates
     * - Request to support multiple clients for the same UISurface.
     */
    @Nullable
    private final Bundle mExtras;

    private SmartspaceConfig(@NonNull String uiSurface, int numPredictedTargets,
            @NonNull String packageName, @Nullable Bundle extras) {
        throw new RuntimeException("Stub!");
    }

    private SmartspaceConfig(Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    /** Returns the package name of the prediction context. */
    @NonNull
    public String getPackageName() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the number of smartspace targets requested by the user. */
    @NonNull
    public int getSmartspaceTargetCount() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the UISurface requested by the client. */
    @NonNull
    public String getUiSurface() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    @SuppressLint("NullableCollection")
    public Bundle getExtras() {
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

    @Override
    public boolean equals(Object o) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @see Creator
     */
    @NonNull
    public static final Creator<SmartspaceConfig> CREATOR =
            new Creator<SmartspaceConfig>() {
                public SmartspaceConfig createFromParcel(Parcel parcel) {
                    return new SmartspaceConfig(parcel);
                }

                public SmartspaceConfig[] newArray(int size) {
                    return new SmartspaceConfig[size];
                }
            };

    /**
     * A builder for {@link SmartspaceConfig}.
     *
     * @hide
     */
    public static final class Builder {
        @NonNull
        private int mSmartspaceTargetCount = 5; // Default count is 5
        @NonNull
        private final String mUiSurface;
        @NonNull
        private final String mPackageName;
        @NonNull
        private Bundle mExtras = Bundle.EMPTY;

        /**
         * @param context   The {@link Context} which is used to fetch the package name.
         * @param uiSurface the UI Surface name associated with this context.
         */
        public Builder(@NonNull Context context, @NonNull String uiSurface) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Used to set the expected number of cards for this context.
         */
        @NonNull
        public Builder setSmartspaceTargetCount(
                @IntRange(from = 0, to = 50) int smartspaceTargetCount) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Used to send a bundle containing extras for the {@link SmartspaceConfig}.
         */
        @NonNull
        public Builder setExtras(@SuppressLint("NullableCollection") @NonNull Bundle extras) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Returns an instance of {@link SmartspaceConfig}.
         */
        @NonNull
        public SmartspaceConfig build() {
            throw new RuntimeException("Stub!");
        }
    }
}
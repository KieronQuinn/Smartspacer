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

import android.content.pm.ShortcutInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * A representation of a launchable target.
 */
public final class AppTarget implements Parcelable {

    /**
     * @deprecated use the Builder class
     */
    @Deprecated
    public AppTarget(@NonNull AppTargetId id, @NonNull String packageName,
                     @Nullable String className, @NonNull UserHandle user) {
        throw new RuntimeException("Stub!");
    }

    /**
     * @deprecated use the Builder class
     */
    @Deprecated
    public AppTarget(@NonNull AppTargetId id, @NonNull ShortcutInfo shortcutInfo,
            @Nullable String className) {
        throw new RuntimeException("Stub!");
    }

    private AppTarget(AppTargetId id, String packageName, UserHandle user,
            ShortcutInfo shortcutInfo, String className, int rank) {
        throw new RuntimeException("Stub!");
    }

    private AppTarget(Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the target id.
     */
    @NonNull
    public AppTargetId getId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the class name for the app target.
     */
    @Nullable
    public String getClassName() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the package name for the app target.
     */
    @NonNull
    public String getPackageName() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the user for the app target.
     */
    @NonNull
    public UserHandle getUser() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the shortcut info for the target.
     */
    @Nullable
    public ShortcutInfo getShortcutInfo() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the rank for the target. Rank of an AppTarget is a non-negative integer that
     * represents the importance of this target compared to other candidate targets. A smaller value
     * means higher importance in the list.
     */
    public @IntRange(from = 0) int getRank() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean equals(@Nullable Object o) {
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

    /**
     * A builder for app targets.
     */
    public static final class Builder {

        /**
         * @deprecated Use the other Builder constructors.
         */
        @Deprecated
        public Builder(@NonNull AppTargetId id) {
            throw new RuntimeException("Stub!");
        }

        /**
         * @param id A unique id for this launchable target.
         * @param packageName PackageName of the target.
         * @param user The UserHandle of the user which this target belongs to.
         */
        public Builder(@NonNull AppTargetId id, @NonNull String packageName,
                @NonNull UserHandle user) {
            throw new RuntimeException("Stub!");
        }

        /**
         * @param id A unique id for this launchable target.
         * @param info The ShortcutInfo that represents this launchable target.
         */
        public Builder(@NonNull AppTargetId id, @NonNull ShortcutInfo info) {
            throw new RuntimeException("Stub!");
        }

        /**
         * @deprecated Use the appropriate constructor.
         */
        @NonNull
        @Deprecated
        public Builder setTarget(@NonNull String packageName, @NonNull UserHandle user) {
            throw new RuntimeException("Stub!");
        }

        /**
         * @deprecated Use the appropriate constructor.
         */
        @NonNull
        @Deprecated
        public Builder setTarget(@NonNull ShortcutInfo info) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the className for the target.
         */
        @NonNull
        public Builder setClassName(@NonNull String className) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the rank of the target.
         */
        @NonNull
        public Builder setRank(@IntRange(from = 0) int rank) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Builds a new AppTarget instance.
         *
         * @throws IllegalStateException if no target is set
         * @see #setTarget(ShortcutInfo)
         * @see #setTarget(String, UserHandle)
         */
        @NonNull
        public AppTarget build() {
            throw new RuntimeException("Stub!");
        }
    }

    public static final @androidx.annotation.NonNull Parcelable.Creator<AppTarget> CREATOR =
            new Parcelable.Creator<AppTarget>() {
                public AppTarget createFromParcel(Parcel parcel) {
                    return new AppTarget(parcel);
                }

                public AppTarget[] newArray(int size) {
                    return new AppTarget[size];
                }
            };
}
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

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class that provides contextual information about the environment in which the app prediction is
 * used, such as package name, UI in which the app targets are shown, and number of targets.
 */
public final class AppPredictionContext implements Parcelable {

    private AppPredictionContext(@NonNull String uiSurface, int numPredictedTargets,
                                 @NonNull String packageName, @Nullable Bundle extras) {
        throw new RuntimeException("Stub!");
    }

    private AppPredictionContext(@NonNull Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the UI surface of the prediction context.
     */
    @NonNull
    public String getUiSurface() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the predicted target count
     */
    public @IntRange(from = 0) int getPredictedTargetCount() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the package name of the prediction context.
     */
    @NonNull
    public String getPackageName() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the extras of the prediction context.
     */
    @Nullable
    public Bundle getExtras() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    public static final @androidx.annotation.NonNull Parcelable.Creator<AppPredictionContext> CREATOR =
            new Parcelable.Creator<AppPredictionContext>() {
                public AppPredictionContext createFromParcel(Parcel parcel) {
                    return new AppPredictionContext(parcel);
                }

                public AppPredictionContext[] newArray(int size) {
                    return new AppPredictionContext[size];
                }
            };

    /**
     * A builder for app prediction contexts.
     */
    public static final class Builder {

        @NonNull
        private final String mPackageName;

        private int mPredictedTargetCount;
        @Nullable
        private String mUiSurface;
        @Nullable
        private Bundle mExtras;

        /**
         * @param context The {@link Context} of the prediction client.
         */
        public Builder(@NonNull Context context) {
            throw new RuntimeException("Stub!");
        }


        /**
         * Sets the number of prediction targets as a hint.
         */
        @NonNull
        public Builder setPredictedTargetCount(@IntRange(from = 0) int predictedTargetCount) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the UI surface.
         */
        @NonNull
        public Builder setUiSurface(@NonNull String uiSurface) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the extras.
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Builds a new context instance.
         */
        @NonNull
        public AppPredictionContext build() {
            throw new RuntimeException("Stub!");
        }
    }
}
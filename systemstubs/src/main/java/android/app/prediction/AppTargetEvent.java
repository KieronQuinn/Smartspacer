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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A representation of an app target event.
 *
 * @hide
 */
public final class AppTargetEvent implements Parcelable {

    /**
     * @hide
     */
    @IntDef({ACTION_LAUNCH, ACTION_DISMISS, ACTION_PIN, ACTION_UNPIN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {}

    /**
     * Event type constant indicating an app target has been launched.
     */
    public static final int ACTION_LAUNCH = 1;

    /**
     * Event type constant indicating an app target has been dismissed.
     */
    public static final int ACTION_DISMISS = 2;

    /**
     * Event type constant indicating an app target has been pinned.
     */
    public static final int ACTION_PIN = 3;

    /**
     * Event type constant indicating an app target has been un-pinned.
     */
    public static final int ACTION_UNPIN = 4;

    private AppTargetEvent(@Nullable AppTarget target, @Nullable String location,
                           @ActionType int actionType) {
        throw new RuntimeException("Stub!");
    }

    private AppTargetEvent(Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the app target.
     */
    @Nullable
    public AppTarget getTarget() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the launch location.
     */
    @Nullable
    public String getLaunchLocation() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the action type.
     */
    public @ActionType int getAction() {
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

    public static final @androidx.annotation.NonNull Creator<AppTargetEvent> CREATOR =
            new Creator<AppTargetEvent>() {
                public AppTargetEvent createFromParcel(Parcel parcel) {
                    return new AppTargetEvent(parcel);
                }

                public AppTargetEvent[] newArray(int size) {
                    return new AppTargetEvent[size];
                }
            };

    /**
     * A builder for app target events.
     */
    public static final class Builder {

        /**
         * @param target The app target that is associated with this event.
         * @param actionType The event type, which is one of the values in {@link ActionType}.
         */
        public Builder(@Nullable AppTarget target, @ActionType int actionType) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the launch location.
         */
        @NonNull
        public Builder setLaunchLocation(@Nullable String location) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Builds a new event instance.
         */
        @NonNull
        public AppTargetEvent build() {
            throw new RuntimeException("Stub!");
        }
    }
}
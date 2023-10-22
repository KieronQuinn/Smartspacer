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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A representation of a smartspace event.
 *
 */
public final class SmartspaceTargetEvent implements Parcelable {

    /**
     * User interacted with the target.
     */
    public static final int EVENT_TARGET_INTERACTION = 1;

    /**
     * Smartspace target was brought into view.
     */
    public static final int EVENT_TARGET_SHOWN = 2;
    /**
     * Smartspace target went out of view.
     */
    public static final int EVENT_TARGET_HIDDEN = 3;
    /**
     * A dismiss action was issued by the user.
     */
    public static final int EVENT_TARGET_DISMISS = 4;
    /**
     * A block action was issued by the user.
     */
    public static final int EVENT_TARGET_BLOCK = 5;
    /**
     * The Ui surface came into view.
     */
    public static final int EVENT_UI_SURFACE_SHOWN = 6;
    /**
     * The Ui surface went out of view.
     */
    public static final int EVENT_UI_SURFACE_HIDDEN = 7;

    /**
     * @see Creator
     */
    @NonNull
    public static final Creator<SmartspaceTargetEvent> CREATOR =
            new Creator<SmartspaceTargetEvent>() {
                public SmartspaceTargetEvent createFromParcel(Parcel parcel) {
                    return new SmartspaceTargetEvent(parcel);
                }

                public SmartspaceTargetEvent[] newArray(int size) {
                    return new SmartspaceTargetEvent[size];
                }
            };

    @Nullable
    private final SmartspaceTarget mSmartspaceTarget;

    @Nullable
    private final String mSmartspaceActionId;

    @EventType
    private final int mEventType;

    private SmartspaceTargetEvent(@Nullable SmartspaceTarget smartspaceTarget,
            @Nullable String smartspaceActionId,
            @EventType int eventType) {
        throw new RuntimeException("Stub!");
    }

    private SmartspaceTargetEvent(Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Get the {@link SmartspaceTarget} associated with this event.
     */
    @Nullable
    public SmartspaceTarget getSmartspaceTarget() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Get the action id of the Smartspace Action associated with this event.
     */
    @Nullable
    public String getSmartspaceActionId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Get the {@link EventType} of this event.
     */
    @NonNull
    @EventType
    public int getEventType() {
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
    public String toString() {
        throw new RuntimeException("Stub!");
    }

    @IntDef(value = {
            EVENT_TARGET_INTERACTION,
            EVENT_TARGET_SHOWN,
            EVENT_TARGET_HIDDEN,
            EVENT_TARGET_DISMISS,
            EVENT_TARGET_BLOCK,
            EVENT_UI_SURFACE_SHOWN,
            EVENT_UI_SURFACE_HIDDEN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EventType {
    }

    /**
     * A builder for {@link SmartspaceTargetEvent}.
     *
     */
    public static final class Builder {
        @EventType
        private final int mEventType;
        @Nullable
        private SmartspaceTarget mSmartspaceTarget;
        @Nullable
        private String mSmartspaceActionId;

        /**
         * A builder for {@link SmartspaceTargetEvent}.
         */
        public Builder(@EventType int eventType) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the SmartspaceTarget for this event.
         */
        @NonNull
        public Builder setSmartspaceTarget(@NonNull SmartspaceTarget smartspaceTarget) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the Smartspace action id for this event.
         */
        @NonNull
        public Builder setSmartspaceActionId(@NonNull String smartspaceActionId) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Builds a new {@link SmartspaceTargetEvent} instance.
         */
        @NonNull
        public SmartspaceTargetEvent build() {
            throw new RuntimeException("Stub!");
        }
    }
}
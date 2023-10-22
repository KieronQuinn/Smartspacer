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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.TextUtils;

import java.util.Objects;

/**
 * A {@link SmartspaceAction} represents an action which can be taken by a user by tapping on either
 * the title, the subtitle or on the icon. Supported instances are Intents, PendingIntents or a
 * ShortcutInfo (by putting the ShortcutInfoId in the bundle). These actions can be called from
 * another process or within the client process.
 *
 * Clients can also receive conditional Intents/PendingIntents in the extras bundle which are
 * supposed to be fired when the conditions are met. For example, a user can invoke a dismiss/block
 * action on a game score card but the intention is to only block the team and not the entire
 * feature.
 */
public final class SmartspaceAction implements Parcelable {

    private static final String TAG = "SmartspaceAction";

    /** A unique Id of this {@link SmartspaceAction}. */
    @NonNull
    private final String mId;

    /** An Icon which can be displayed in the UI. */
    @Nullable
    private final Icon mIcon;

    /** Title associated with an action. */
    @NonNull
    private final CharSequence mTitle;

    /** Subtitle associated with an action. */
    @Nullable
    private final CharSequence mSubtitle;

    @Nullable
    private final CharSequence mContentDescription;

    @Nullable
    private final PendingIntent mPendingIntent;

    @Nullable
    private final Intent mIntent;

    @Nullable
    private final UserHandle mUserHandle;

    @Nullable
    private Bundle mExtras;

    SmartspaceAction(Parcel in) {
        throw new RuntimeException("Stub!");
    }

    private SmartspaceAction(
            @NonNull String id,
            @Nullable Icon icon,
            @NonNull CharSequence title,
            @Nullable CharSequence subtitle,
            @Nullable CharSequence contentDescription,
            @Nullable PendingIntent pendingIntent,
            @Nullable Intent intent,
            @Nullable UserHandle userHandle,
            @Nullable Bundle extras) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the unique id of this object.
     */
    public @NonNull String getId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns an icon representing the action.
     */
    public @Nullable Icon getIcon() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns a title representing the action.
     */
    public @NonNull CharSequence getTitle() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns a subtitle representing the action.
     */
    public @Nullable CharSequence getSubtitle() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns a content description representing the action.
     */
    public @Nullable CharSequence getContentDescription() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the action intent.
     */
    public @Nullable PendingIntent getPendingIntent() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the intent.
     */
    public @Nullable Intent getIntent() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the user handle.
     */
    public @Nullable UserHandle getUserHandle() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the extra bundle for this object.
     */
    @SuppressLint("NullableCollection")
    public @Nullable Bundle getExtras() {
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

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String toString() {
        throw new RuntimeException("Stub!");
    }

    public static final @NonNull Creator<SmartspaceAction> CREATOR =
            new Creator<SmartspaceAction>() {
                public SmartspaceAction createFromParcel(Parcel in) {
                    return new SmartspaceAction(in);
                }
                public SmartspaceAction[] newArray(int size) {
                    return new SmartspaceAction[size];
                }
            };

    /**
     * A builder for Smartspace action object.
     */
    public static final class Builder {
        @NonNull
        private String mId;

        @Nullable
        private Icon mIcon;

        @NonNull
        private CharSequence mTitle;

        @Nullable
        private CharSequence mSubtitle;

        @Nullable
        private CharSequence mContentDescription;

        @Nullable
        private PendingIntent mPendingIntent;

        @Nullable
        private Intent mIntent;

        @Nullable
        private UserHandle mUserHandle;

        @Nullable
        private Bundle mExtras;

        /**
         * Id and title are required.
         */
        public Builder(@NonNull String id, @NonNull String title) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the icon.
         */
        @NonNull
        public Builder setIcon(
                @Nullable Icon icon) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the subtitle.
         */
        @NonNull
        public Builder setSubtitle(
                @Nullable CharSequence subtitle) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the content description.
         */
        @NonNull
        public Builder setContentDescription(
                @Nullable CharSequence contentDescription) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the pending intent.
         */
        @NonNull
        public Builder setPendingIntent(@Nullable PendingIntent pendingIntent) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the user handle.
         */
        @NonNull
        public Builder setUserHandle(@Nullable UserHandle userHandle) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the intent.
         */
        @NonNull
        public Builder setIntent(@Nullable Intent intent) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the extra.
         */
        @NonNull
        public Builder setExtras(@SuppressLint("NullableCollection") @Nullable Bundle extras) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Builds a new SmartspaceAction instance.
         *
         * @throws IllegalStateException if no target is set
         */
        @NonNull
        public SmartspaceAction build() {
            throw new RuntimeException("Stub!");
        }
    }
}
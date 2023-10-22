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

import android.app.smartspace.uitemplatedata.BaseTemplateData;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A {@link SmartspaceTarget} is a data class which holds all properties necessary to inflate a
 * smartspace card. It contains data and related metadata which is supposed to be utilized by
 * smartspace clients based on their own UI/UX requirements. Some of the properties have
 * {@link SmartspaceAction} as their type because they can have associated actions.
 *
 * <p><b>NOTE: </b>
 * If {mWidget} is set, it should be preferred over all other properties.
 * Else, if {mSliceUri} is set, it should be preferred over all other data properties.
 * Otherwise, the instance should be treated as a data object.
 *
 */
public final class SmartspaceTarget implements Parcelable {

    public static final int FEATURE_UNDEFINED = 0;
    public static final int FEATURE_WEATHER = 1;
    public static final int FEATURE_CALENDAR = 2;
    public static final int FEATURE_COMMUTE_TIME = 3;
    public static final int FEATURE_FLIGHT = 4;
    public static final int FEATURE_TIPS = 5;
    public static final int FEATURE_REMINDER = 6;
    public static final int FEATURE_ALARM = 7;
    public static final int FEATURE_ONBOARDING = 8;
    public static final int FEATURE_SPORTS = 9;
    public static final int FEATURE_WEATHER_ALERT = 10;
    public static final int FEATURE_CONSENT = 11;
    public static final int FEATURE_STOCK_PRICE_CHANGE = 12;
    public static final int FEATURE_SHOPPING_LIST = 13;
    public static final int FEATURE_LOYALTY_CARD = 14;
    public static final int FEATURE_MEDIA = 15;
    public static final int FEATURE_BEDTIME_ROUTINE = 16;
    public static final int FEATURE_FITNESS_TRACKING = 17;
    public static final int FEATURE_ETA_MONITORING = 18;
    public static final int FEATURE_MISSED_CALL = 19;
    public static final int FEATURE_PACKAGE_TRACKING = 20;
    public static final int FEATURE_TIMER = 21;
    public static final int FEATURE_STOPWATCH = 22;
    public static final int FEATURE_UPCOMING_ALARM = 23;

    @IntDef(value = {
            FEATURE_UNDEFINED,
            FEATURE_WEATHER,
            FEATURE_CALENDAR,
            FEATURE_COMMUTE_TIME,
            FEATURE_FLIGHT,
            FEATURE_TIPS,
            FEATURE_REMINDER,
            FEATURE_ALARM,
            FEATURE_ONBOARDING,
            FEATURE_SPORTS,
            FEATURE_WEATHER_ALERT,
            FEATURE_CONSENT,
            FEATURE_STOCK_PRICE_CHANGE,
            FEATURE_SHOPPING_LIST,
            FEATURE_LOYALTY_CARD,
            FEATURE_MEDIA,
            FEATURE_BEDTIME_ROUTINE,
            FEATURE_FITNESS_TRACKING,
            FEATURE_ETA_MONITORING,
            FEATURE_MISSED_CALL,
            FEATURE_PACKAGE_TRACKING,
            FEATURE_TIMER,
            FEATURE_STOPWATCH,
            FEATURE_UPCOMING_ALARM
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FeatureType {
    }

    public static final int UI_TEMPLATE_UNDEFINED = 0;
    // Default template whose data is represented by {@link BaseTemplateData}. The default
    // template is also a base card for the other types of templates.
    public static final int UI_TEMPLATE_DEFAULT = 1;
    // Sub-image template whose data is represented by {@link SubImageTemplateData}
    public static final int UI_TEMPLATE_SUB_IMAGE = 2;
    // Sub-list template whose data is represented by {@link SubListTemplateData}
    public static final int UI_TEMPLATE_SUB_LIST = 3;
    // Carousel template whose data is represented by {@link CarouselTemplateData}
    public static final int UI_TEMPLATE_CAROUSEL = 4;
    // Head-to-head template whose data is represented by {@link HeadToHeadTemplateData}
    public static final int UI_TEMPLATE_HEAD_TO_HEAD = 5;
    // Combined-cards template whose data is represented by {@link CombinedCardsTemplateData}
    public static final int UI_TEMPLATE_COMBINED_CARDS = 6;
    // Sub-card template whose data is represented by {@link SubCardTemplateData}
    public static final int UI_TEMPLATE_SUB_CARD = 7;

    /**
     * The types of the Smartspace ui templates.
     */
    @IntDef(value = {
            UI_TEMPLATE_UNDEFINED,
            UI_TEMPLATE_DEFAULT,
            UI_TEMPLATE_SUB_IMAGE,
            UI_TEMPLATE_SUB_LIST,
            UI_TEMPLATE_CAROUSEL,
            UI_TEMPLATE_HEAD_TO_HEAD,
            UI_TEMPLATE_COMBINED_CARDS,
            UI_TEMPLATE_SUB_CARD
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UiTemplateType {
    }

    private SmartspaceTarget(Parcel in) {
        throw new RuntimeException("Stub!");
    }

    private SmartspaceTarget(String smartspaceTargetId,
            SmartspaceAction headerAction, SmartspaceAction baseAction, long creationTimeMillis,
            long expiryTimeMillis, float score,
            List<SmartspaceAction> actionChips,
            List<SmartspaceAction> iconGrid, int featureType, boolean sensitive,
            boolean shouldShowExpanded, String sourceNotificationKey,
            ComponentName componentName, UserHandle userHandle,
            String associatedSmartspaceTargetId, Uri sliceUri,
            AppWidgetProviderInfo widget) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the Id of the target.
     */
    @NonNull
    public String getSmartspaceTargetId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the header action of the target.
     */
    @Nullable
    public SmartspaceAction getHeaderAction() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the base action of the target.
     */
    @Nullable
    public SmartspaceAction getBaseAction() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the creation time of the target.
     */

    public long getCreationTimeMillis() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the expiry time of the target.
     */
    public long getExpiryTimeMillis() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the score of the target.
     */
    public float getScore() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Return the action chips of the target.
     */
    @NonNull
    public List<SmartspaceAction> getActionChips() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Return the icons of the target.
     */
    @NonNull
    public List<SmartspaceAction> getIconGrid() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the feature type of the target.
     */
    @FeatureType
    public int getFeatureType() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns whether the target is sensitive or not.
     */
    public boolean isSensitive() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns whether the target should be shown in expanded state.
     */
    public boolean shouldShowExpanded() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the source notification key of the target.
     */
    @Nullable
    public String getSourceNotificationKey() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the component name of the target.
     */
    @NonNull
    public ComponentName getComponentName() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the user handle of the target.
     */
    @NonNull
    public UserHandle getUserHandle() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the id of a target associated with this instance.
     */
    @Nullable
    public String getAssociatedSmartspaceTargetId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the slice uri, if the target is a slice.
     */
    @Nullable
    public Uri getSliceUri() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the AppWidgetProviderInfo, if the target is a widget.
     */
    @Nullable
    public AppWidgetProviderInfo getWidget() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the template data, if it is set
     */
    @Nullable
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public BaseTemplateData getTemplateData() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @see Creator
     */
    @NonNull
    public static final Creator<SmartspaceTarget> CREATOR = new Creator<SmartspaceTarget>() {
        @Override
        public SmartspaceTarget createFromParcel(Parcel source) {
            return new SmartspaceTarget(source);
        }

        @Override
        public SmartspaceTarget[] newArray(int size) {
            return new SmartspaceTarget[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String toString() {
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
     * A builder for {@link SmartspaceTarget} object.
     *
     */
    public static final class Builder {

        /**
         * A builder for {@link SmartspaceTarget}.
         *
         * @param smartspaceTargetId the id of this target
         * @param componentName      the componentName of this target
         * @param userHandle         the userHandle of this target
         */
        public Builder(@NonNull String smartspaceTargetId,
                @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the header action.
         */
        @NonNull
        public Builder setHeaderAction(@NonNull SmartspaceAction headerAction) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the base action.
         */
        @NonNull
        public Builder setBaseAction(@NonNull SmartspaceAction baseAction) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the creation time.
         */
        @NonNull
        public Builder setCreationTimeMillis(long creationTimeMillis) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the expiration time.
         */
        @NonNull
        public Builder setExpiryTimeMillis(long expiryTimeMillis) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the score.
         */
        @NonNull
        public Builder setScore(float score) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the action chips.
         */
        @NonNull
        public Builder setActionChips(@NonNull List<SmartspaceAction> actionChips) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the icon grid.
         */
        @NonNull
        public Builder setIconGrid(@NonNull List<SmartspaceAction> iconGrid) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the feature type.
         */
        @NonNull
        public Builder setFeatureType(int featureType) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets whether the contents are sensitive.
         */
        @NonNull
        public Builder setSensitive(boolean sensitive) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets whether to show the card as expanded.
         */
        @NonNull
        public Builder setShouldShowExpanded(boolean shouldShowExpanded) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the source notification key.
         */
        @NonNull
        public Builder setSourceNotificationKey(@NonNull String sourceNotificationKey) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the associated smartspace target id.
         */
        @NonNull
        public Builder setAssociatedSmartspaceTargetId(
                @NonNull String associatedSmartspaceTargetId) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the slice uri.
         *
         * <p><b>NOTE: </b> If {mWidget} is also set, {mSliceUri} should be ignored.
         */
        @NonNull
        public Builder setSliceUri(@NonNull Uri sliceUri) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the widget id.
         *
         * <p><b>NOTE: </b> If {mWidget} is set, all other @Nullable params should be
         * ignored.
         */
        @NonNull
        public Builder setWidget(@NonNull AppWidgetProviderInfo widget) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Sets the template data
         *
         * <p><b>NOTE: </b> If {mTemplateData} is set, legacy data will be ignored
         */
        @NonNull
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public Builder setTemplateData(@NonNull  BaseTemplateData templateData) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Builds a new {@link SmartspaceTarget}.
         *
         * @throws IllegalStateException when non null fields are set as null.
         */
        @NonNull
        public SmartspaceTarget build() {
            throw new RuntimeException("Stub!");
        }
    }
}
package android.app.smartspace.uitemplatedata;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import androidx.annotation.Nullable;

public class TapAction implements Parcelable {

    public static final class Builder {
        public Builder(CharSequence id) {
            throw new RuntimeException("Stub!");
        }

        public TapAction build() {
            throw new RuntimeException("Stub!");
        }

        public Builder setExtras(@Nullable Bundle extras) {
            throw new RuntimeException("Stub!");
        }

        public Builder setIntent(@Nullable Intent intent) {
            throw new RuntimeException("Stub!");
        }

        public Builder setPendingIntent(@Nullable PendingIntent pendingIntent) {
            throw new RuntimeException("Stub!");
        }

        public Builder setShouldShowOnLockscreen(boolean shouldShowOnLockScreen) {
            throw new RuntimeException("Stub!");
        }

        public Builder setUserHandle(UserHandle userHandle) {
            throw new RuntimeException("Stub!");
        }
    }

    public Bundle getExtras() {
        throw new RuntimeException("Stub!");
    }

    public CharSequence getId() {
        throw new RuntimeException("Stub!");
    }

    public Intent getIntent() {
        throw new RuntimeException("Stub!");
    }

    public PendingIntent getPendingIntent() {
        throw new RuntimeException("Stub!");
    }

    public UserHandle getUserHandle() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("Stub!");
    }

    public boolean shouldShowOnLockscreen() {
        throw new RuntimeException("Stub!");
    }

    protected TapAction(Parcel in) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<TapAction> CREATOR = new Creator<TapAction>() {
        @Override
        public TapAction createFromParcel(Parcel in) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public TapAction[] newArray(int size) {
            throw new RuntimeException("Stub!");
        }
    };

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        throw new RuntimeException("Stub!");
    }
}

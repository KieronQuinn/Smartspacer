package android.app.smartspace.uitemplatedata;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class Icon implements Parcelable {

    public static final class Builder {
        public Builder(android.graphics.drawable.Icon icon) {
            throw new RuntimeException("Stub!");
        }

        public Icon build() {
            throw new RuntimeException("Stub!");
        }

        public Builder setContentDescription(@Nullable CharSequence contentDescription) {
            throw new RuntimeException("Stub!");
        }

        public Builder setShouldTint(boolean shouldTint) {
            throw new RuntimeException("Stub!");
        }
    }

    public CharSequence getContentDescription() {
        throw new RuntimeException("Stub!");
    }

    public android.graphics.drawable.Icon getIcon() {
        throw new RuntimeException("Stub!");
    }

    public boolean shouldTint() {
        throw new RuntimeException("Stub!");
    }

    protected Icon(Parcel in) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<Icon> CREATOR = new Creator<Icon>() {
        @Override
        public Icon createFromParcel(Parcel in) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public Icon[] newArray(int size) {
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

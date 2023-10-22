package android.app.smartspace.uitemplatedata;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils.TruncateAt;

public class Text implements Parcelable {

    public static final class Builder {
        public Builder(CharSequence text) {
            throw new RuntimeException("Stub!");
        }

        public Text build() {
            throw new RuntimeException("Stub!");
        }

        public Builder setMaxLines(int maxLines) {
            throw new RuntimeException("Stub!");
        }

        public Builder setTruncateAtType(TruncateAt truncateAtType) {
            throw new RuntimeException("Stub!");
        }
    }

    public int getMaxLines() {
        throw new RuntimeException("Stub!");
    }

    public CharSequence getText() {
        throw new RuntimeException("Stub!");
    }

    public TruncateAt getTruncateAtType() {
        throw new RuntimeException("Stub!");
    }

    protected Text(Parcel in) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<Text> CREATOR = new Creator<Text>() {
        @Override
        public Text createFromParcel(Parcel in) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public Text[] newArray(int size) {
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

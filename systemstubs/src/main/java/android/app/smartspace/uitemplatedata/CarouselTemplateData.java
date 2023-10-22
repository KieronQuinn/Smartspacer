package android.app.smartspace.uitemplatedata;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.List;

public class CarouselTemplateData extends BaseTemplateData {

    public static final class Builder extends BaseTemplateData.Builder {
        public Builder(List<CarouselItem> items) {
            super(4);
            throw new RuntimeException("Stub!");
        }

        @Override
        public CarouselTemplateData build() {
            throw new RuntimeException("Stub!");
        }

        public Builder setCarouselAction(TapAction carouselAction) {
            throw new RuntimeException("Stub!");
        }
    }

    public static final class CarouselItem implements Parcelable {

        public static final class Builder {
            public CarouselItem build() {
                throw new RuntimeException("Stub!");
            }

            public Builder setImage(@Nullable Icon image) {
                throw new RuntimeException("Stub!");
            }

            public Builder setLowerText(@Nullable Text lowerText) {
                throw new RuntimeException("Stub!");
            }

            public Builder setTapAction(@Nullable TapAction tapAction) {
                throw new RuntimeException("Stub!");
            }

            public Builder setUpperText(@Nullable Text upperText) {
                throw new RuntimeException("Stub!");
            }
        }

        public Icon getImage() {
            throw new RuntimeException("Stub!");
        }

        public Text getLowerText() {
            throw new RuntimeException("Stub!");
        }

        public TapAction getTapAction() {
            throw new RuntimeException("Stub!");
        }

        public Text getUpperText() {
            throw new RuntimeException("Stub!");
        }

        protected CarouselItem(Parcel in) {
            throw new RuntimeException("Stub!");
        }

        public static final Creator<CarouselItem> CREATOR = new Creator<CarouselItem>() {
            @Override
            public CarouselItem createFromParcel(Parcel in) {
                throw new RuntimeException("Stub!");
            }

            @Override
            public CarouselItem[] newArray(int size) {
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

    protected CarouselTemplateData(Parcel in) {
        super(in);
    }

    public TapAction getCarouselAction() {
        throw new RuntimeException("Stub!");
    }

    public List<CarouselItem> getCarouselItems() {
        throw new RuntimeException("Stub!");
    }

}

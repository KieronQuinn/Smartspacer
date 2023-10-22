package android.app.smartspace.uitemplatedata;

import android.os.Parcel;

import java.util.List;

public class SubImageTemplateData extends BaseTemplateData {

    public static final class Builder extends BaseTemplateData.Builder {
        public Builder(List<Text> subImageTexts, List<Icon> subImages) {
            super(2);
        }

        public Builder setSubImageAction(TapAction subImageAction) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public SubImageTemplateData build() {
            throw new RuntimeException("Stub!");
        }
    }

    public TapAction getSubImageAction() {
        throw new RuntimeException("Stub!");
    }

    public List<Text> getSubImageTexts() {
        throw new RuntimeException("Stub!");
    }

    public List<Icon> getSubImages() {
        throw new RuntimeException("Stub!");
    }

    protected SubImageTemplateData(Parcel in) {
        super(in);
    }

}

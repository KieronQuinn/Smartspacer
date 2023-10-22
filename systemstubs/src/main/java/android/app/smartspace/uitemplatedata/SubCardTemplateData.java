package android.app.smartspace.uitemplatedata;

import android.os.Parcel;

public class SubCardTemplateData extends BaseTemplateData {

    public static final class Builder extends BaseTemplateData.Builder {
        public Builder(Icon subCardIcon) {
            super(7);
        }

        @Override
        public SubCardTemplateData build() {
            throw new RuntimeException("Stub!");
        }

        public Builder setSubCardAction(TapAction subCardAction) {
            throw new RuntimeException("Stub!");
        }

        public Builder setSubCardText(Text subCardText) {
            throw new RuntimeException("Stub!");
        }
    }

    public TapAction getSubCardAction() {
        throw new RuntimeException("Stub!");
    }

    public Icon getSubCardIcon() {
        throw new RuntimeException("Stub!");
    }

    public Text getSubCardText() {
        throw new RuntimeException("Stub!");
    }

    protected SubCardTemplateData(Parcel in) {
        super(in);
    }

}

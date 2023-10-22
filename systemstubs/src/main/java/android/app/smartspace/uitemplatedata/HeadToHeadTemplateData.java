package android.app.smartspace.uitemplatedata;

import android.os.Parcel;

public class HeadToHeadTemplateData extends BaseTemplateData {

    public static final class Builder extends BaseTemplateData.Builder {
        public Builder() {
            super(5);
        }

        @Override
        public HeadToHeadTemplateData build() {
            throw new RuntimeException("Stub!");
        }

        public Builder setHeadToHeadAction(TapAction headToHeadAction) {
            throw new RuntimeException("Stub!");
        }

        public Builder setHeadToHeadFirstCompetitorIcon(Icon headToHeadFirstCompetitorIcon) {
            throw new RuntimeException("Stub!");
        }

        public Builder setHeadToHeadFirstCompetitorText(Text headToHeadFirstCompetitorText) {
            throw new RuntimeException("Stub!");
        }

        public Builder setHeadToHeadSecondCompetitorIcon(Icon headToHeadSecondCompetitorIcon) {
            throw new RuntimeException("Stub!");
        }

        public Builder setHeadToHeadSecondCompetitorText(Text headToHeadSecondCompetitorText) {
            throw new RuntimeException("Stub!");
        }

        public Builder setHeadToHeadTitle(Text headToHeadTitle) {
            throw new RuntimeException("Stub!");
        }
    }

    public TapAction getHeadToHeadAction() {
        throw new RuntimeException("Stub!");
    }

    public Icon getHeadToHeadFirstCompetitorIcon() {
        throw new RuntimeException("Stub!");
    }

    public Text getHeadToHeadFirstCompetitorText() {
        throw new RuntimeException("Stub!");
    }

    public Icon getHeadToHeadSecondCompetitorIcon() {
        throw new RuntimeException("Stub!");
    }

    public Text getHeadToHeadSecondCompetitorText() {
        throw new RuntimeException("Stub!");
    }

    public Text getHeadToHeadTitle() {
        throw new RuntimeException("Stub!");
    }

    protected HeadToHeadTemplateData(Parcel in) {
        super(in);
    }
}

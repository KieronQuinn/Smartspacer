package android.appwidget;

import android.content.pm.ActivityInfo;
import android.util.DisplayMetrics;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppWidgetProviderInfo.class)
public class AppWidgetProviderInfoHidden {

    public void updateDimensions(DisplayMetrics metrics) {
        throw new RuntimeException("Stub!");
    }

    public ActivityInfo providerInfo;

}

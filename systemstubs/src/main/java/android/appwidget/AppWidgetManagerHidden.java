package android.appwidget;

import android.app.IServiceConnection;
import android.content.Context;
import android.content.Intent;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppWidgetManager.class)
public class AppWidgetManagerHidden {

    public boolean bindRemoteViewsService(Context context, int appWidgetId, Intent intent,
                                          IServiceConnection connection, int flags) {
        throw new RuntimeException("Stub!");
    }

}

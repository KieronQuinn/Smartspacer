package android.appwidget;

import android.app.IServiceConnection;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppWidgetManager.class)
public class AppWidgetManagerHidden {

    public boolean bindRemoteViewsService(Context context, int appWidgetId, Intent intent,
                                          IServiceConnection connection, int flags) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Note an app widget is tapped on.
     *
     * @param appWidgetId App widget id.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public void noteAppWidgetTapped(int appWidgetId) {
        throw new RuntimeException("Stub!");
    }

}

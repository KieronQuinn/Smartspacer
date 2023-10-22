package android.appwidget;

import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppWidgetHost.class)
public class AppWidgetHostHidden extends AppWidgetHost {

    public AppWidgetHostHidden(Context context, int hostId) {
        super(context, hostId);
    }

    /**
     * Create an AppWidgetHostListener for the given widget.
     * The AppWidgetHost retains a pointer to the newly-created listener.
     * @param appWidgetId The ID of the app widget for which to add the listener
     * @param listener The listener interface that deals with actions towards the widget view
     */
    public void setListener(int appWidgetId, @NonNull AppWidgetHostListener listener) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Delete the listener for the given widget
     * @param appWidgetId The ID of the app widget for which the listener is to be deleted
     */
    public void removeListener(int appWidgetId) {
        throw new RuntimeException("Stub!");
    }

    public interface AppWidgetHostListener {

        /**
         * This function is called when the service want to reset the app widget provider info
         * @param appWidget The new app widget provider info
         *
         * @hide
         */
        void onUpdateProviderInfo(@Nullable AppWidgetProviderInfo appWidget);

        /**
         * This function is called when the RemoteViews of the app widget is updated
         * @param views The new RemoteViews to be set for the app widget
         *
         * @hide
         */
        void updateAppWidget(@Nullable RemoteViews views);

        /**
         * This function is called when the view ID is changed for the app widget
         * @param viewId The new view ID to be be set for the widget
         *
         * @hide
         */
        void onViewDataChanged(int viewId);
    }

}

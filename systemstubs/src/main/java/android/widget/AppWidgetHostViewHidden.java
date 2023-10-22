package android.widget;

import android.appwidget.AppWidgetHostHidden;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.widget.RemoteViewsHidden.InteractionHandler;

import androidx.annotation.Nullable;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppWidgetHostView.class)
public class AppWidgetHostViewHidden extends AppWidgetHostView implements AppWidgetHostHidden.AppWidgetHostListener {

    public AppWidgetHostViewHidden(Context context) {
        super(context);
    }

    public AppWidgetHostViewHidden(Context context, InteractionHandler interactionHandler) {
        super(context);
    }

    public AppWidgetHostViewHidden(Context context, int animationIn, int animationOut) {
        super(context, animationIn, animationOut);
    }

    @Override
    public void onUpdateProviderInfo(@Nullable AppWidgetProviderInfo appWidget) {
        throw new RuntimeException("Stub!");
    }

    public void onViewDataChanged(int viewId) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Pass the given handler to RemoteViews when updating this widget. Unless this
     * is done immediatly after construction, a call to {@link #updateAppWidget(RemoteViews)}
     * should be made.
     * @param handler
     */
    public void setOnClickHandler(RemoteViewsHidden.OnClickHandler handler) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Pass the given handler to RemoteViews when updating this widget. Unless this
     * is done immediatly after construction, a call to {@link #updateAppWidget(RemoteViews)}
     * should be made.
     * @param handler
     */
    public void setInteractionHandler(InteractionHandler handler) {
        throw new RuntimeException("Stub!");
    }


}

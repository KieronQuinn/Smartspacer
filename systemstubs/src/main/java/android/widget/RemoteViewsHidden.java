package android.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(RemoteViews.class)
public class RemoteViewsHidden extends RemoteViews {

    public RemoteViewsHidden(RemoteViews landscape, RemoteViews portrait) {
        super(landscape, portrait);
    }

    public RemoteViewsHidden(RemoteViews source) {
        super(source);
    }

    public RemoteViewsHidden(String packageName, int layout) {
        super(packageName, layout);
    }

    /**
     * Handler for view interactions (such as clicks) within a RemoteViews.
     */
    public interface InteractionHandler {
        /**
         * Invoked when the user performs an interaction on the View.
         *
         * @param view the View with which the user interacted
         * @param pendingIntent the base PendingIntent associated with the view
         * @param response the response to the interaction, which knows how to fill in the
         *                 attached PendingIntent
         */
        boolean onInteraction(
                View view,
                PendingIntent pendingIntent,
                RemoteResponse response);
    }

    class RemoteCollectionCache {
        RemoteCollectionItems getItemsForId(int intentId) {
            throw new RuntimeException("Stub!");
        }
    }

    public interface OnClickHandler {
        boolean onClickHandler(View view, PendingIntent pendingIntent, RemoteResponse response);
    }

    /**
     * Returns the most appropriate {@link RemoteViews} given the context and, if not null, the
     * size of the widget.
     *
     * If @link RemoteViews#hasSizedRemoteViews() returns true, the most appropriate view is
     * the one that fits in the widget (according to @link RemoteViews#fitsIn) and has the
     * diagonal the most similar to the widget. If no layout fits or the size of the widget is
     * not specified, the one with the smallest area will be chosen.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public RemoteViews getRemoteViewsToApply(@NonNull Context context, @Nullable SizeF widgetSize) {
        throw new RuntimeException("Stub!");
    }

    public void mergeRemoteViews(RemoteViews newRv) {
        throw new RuntimeException("Stub!");
    }

    // < 12
    public View apply(Context context, ViewGroup parent, OnClickHandler handler) {
        throw new RuntimeException("Stub!");
    }

    // >= 12
    public View apply(Context context, ViewGroup parent, InteractionHandler handler) {
        throw new RuntimeException("Stub!");
    }

    public void addFlags(int flags) {
        throw new RuntimeException("Stub!");
    }

    public boolean hasFlags(int flag) {
        throw new RuntimeException("Stub!");
    }

}

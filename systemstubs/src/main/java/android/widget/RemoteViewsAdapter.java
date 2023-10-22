package android.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

/**
 * An adapter to a RemoteViewsService which fetches and caches RemoteViews to be later inflated as
 * child views.
 *
 * The adapter runs in the host process, typically a Launcher app.
 *
 * It makes a service connection to the {@link RemoteViewsService} running in the
 * AppWidgetsProvider's process. This connection is made on a background thread (and proxied via
 * the platform to get the bind permissions) and all interaction with the service is done on the
 * background thread.
 *
 * On first bind, the adapter will load can cache the RemoteViews locally. Afterwards the
 * connection is only made when new RemoteViews are required.
 */
public class RemoteViewsAdapter extends BaseAdapter {

    /**
     * The intent extra that contains the appWidgetId.
     */
    public static final String EXTRA_REMOTEADAPTER_APPWIDGET_ID = "remoteAdapterAppWidgetId";

    /**
     * The intent extra that contains {@code true} if inflating as dak text theme.
     */
    public static final String EXTRA_REMOTEADAPTER_ON_LIGHT_BACKGROUND = "remoteAdapterOnLightBackground";

    /**
     * A FrameLayout which contains a loading view, and manages the re/applying of RemoteViews when
     * they are loaded.
     */
    static class RemoteViewsFrameLayout extends AppWidgetHostView {

        public RemoteViewsFrameLayout(Context context) {
            super(context);
        }

        public RemoteViewsFrameLayout(Context context, int animationIn, int animationOut) {
            super(context, animationIn, animationOut);
        }
    }

    /**
     * An interface for the RemoteAdapter to notify other classes when adapters
     * are actually connected to/disconnected from their actual services.
     */
    public interface RemoteAdapterConnectionCallback {
        /**
         * @return whether the adapter was set or not.
         */
        boolean onRemoteAdapterConnected();

        void onRemoteAdapterDisconnected();

        /**
         * This defers a notifyDataSetChanged on the pending RemoteViewsAdapter if it has not
         * connected yet.
         */
        void deferNotifyDataSetChanged();

        void setRemoteViewsAdapter(Intent intent, boolean isAsync);
    }

    @Override
    public int getCount() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Object getItem(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public long getItemId(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        throw new RuntimeException("Stub!");
    }

    @Override
    protected void finalize() throws Throwable {
        throw new RuntimeException("Stub!");
    }

}

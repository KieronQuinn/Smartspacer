package android.widget;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AbsListView.class)
public class AbsListViewHidden extends AbsListView implements RemoteViewsAdapter.RemoteAdapterConnectionCallback {

    public AbsListViewHidden(Context context) {
        this(context, null);
    }

    public AbsListViewHidden(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.absListViewStyle);
    }

    public AbsListViewHidden(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AbsListViewHidden(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean onRemoteAdapterConnected() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void onRemoteAdapterDisconnected() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void deferNotifyDataSetChanged() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets up this AbsListView to use a remote views adapter which connects to a RemoteViewsService
     * through the specified intent.
     * @param intent the intent used to identify the RemoteViewsService for the adapter to connect to.
     */
    public void setRemoteViewsAdapter(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setRemoteViewsAdapter(Intent intent, boolean isAsync) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ListAdapter getAdapter() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setSelection(int i) {
        throw new RuntimeException("Stub!");
    }

}

package android.widget;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ListView.class)
public class ListViewHidden extends AbsListViewHidden {

    public ListViewHidden(Context context) {
        this(context, null);
    }

    public ListViewHidden(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.listViewStyle);
    }

    public ListViewHidden(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListViewHidden(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets up this AbsListView to use a remote views adapter which connects to a RemoteViewsService
     * through the specified intent.
     * @param intent the intent used to identify the RemoteViewsService for the adapter to connect to.
     */
    public void setRemoteViewsAdapter(Intent intent) {
        super.setRemoteViewsAdapter(intent);
    }

}

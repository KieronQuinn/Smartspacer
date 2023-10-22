package android.content;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Intent.class)
public class IntentHidden {

    public void prepareToLeaveProcess(Context context) {
        throw new RuntimeException("Stub!");
    }

}

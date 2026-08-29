package android.view;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(LayoutInflater.class)
public class LayoutInflaterHidden {

    public void setPrivateFactory(LayoutInflater.Factory2 factory) {
        throw new RuntimeException("Stub!");
    }

}

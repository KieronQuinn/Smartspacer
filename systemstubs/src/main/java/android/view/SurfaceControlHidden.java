package android.view;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SurfaceControl.class)
public class SurfaceControlHidden {

    public static class Transaction {
        public Transaction setBackgroundBlurRadius(SurfaceControl surfaceControl, int radius){
            throw new RuntimeException("Stub!");
        }
    }

}

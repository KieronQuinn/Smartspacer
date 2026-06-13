package android.graphics.drawable;

import android.graphics.Bitmap;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Icon.class)
public class IconHidden {

    public Bitmap getBitmap() {
        throw new RuntimeException("Stub!");
    }

}

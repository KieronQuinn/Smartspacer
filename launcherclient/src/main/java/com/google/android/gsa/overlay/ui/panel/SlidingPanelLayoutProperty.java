package com.google.android.gsa.overlay.ui.panel;

import android.util.Property;

final class SlidingPanelLayoutProperty extends Property {

    SlidingPanelLayoutProperty(Class cls, String str) {
        super(cls, str);
    }

    public final /* synthetic */ Object get(Object obj) {
        return ((SlidingPanelLayout) obj).panelX;
    }

    public final /* synthetic */ void set(Object obj, Object obj2) {
        ((SlidingPanelLayout) obj).BM((Integer) obj2);
    }
}
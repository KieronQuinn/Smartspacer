package com.kieronquinn.app.smartspacer;

import android.content.pm.ParceledListSlice;

interface ISmartspaceOnTargetsAvailableListener {
    void onTargetsAvailable(in ParceledListSlice targets);
}
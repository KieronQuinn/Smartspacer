package com.kieronquinn.app.smartspacer.sdk.client;

import com.kieronquinn.app.smartspacer.sdk.utils.ParceledListSlice;

interface ISmartspaceCallback {

    void onResult(in ParceledListSlice result) = 1;

}
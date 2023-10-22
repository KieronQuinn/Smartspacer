package com.kieronquinn.app.smartspacer;

import android.content.pm.ParceledListSlice;

interface IAppPredictionOnTargetsAvailableListener {

    void onTargetsAvailable(in ParceledListSlice targets);

}
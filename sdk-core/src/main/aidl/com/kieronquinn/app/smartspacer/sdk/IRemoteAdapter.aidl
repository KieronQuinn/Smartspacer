package com.kieronquinn.app.smartspacer.sdk;

interface IRemoteAdapter {
    int getCount() = 1;
    Bundle getViewAt(int index) = 2;
}
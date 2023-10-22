package com.google.android.mediahome.launcheroverlay.aidl;

import com.google.android.mediahome.launcheroverlay.aidl.ILauncherOverlayCallback;

interface ILauncherOverlay {

    oneway void windowAttached(in Bundle options, in ILauncherOverlayCallback callback) = 1; //Bundle should contain layout_params and configuration
    oneway void windowDetached(in boolean isChangingConfigurations) = 2;
    oneway void showOverlay(in Bundle options) = 3; //Bundle should contain overlay_animation_type and overlay_animation_duration
    oneway void hideOverlay(in Bundle options) = 4; //Bundle should contain overlay_animation_type and overlay_animation_duration
    oneway void setActivityState(in int flags) = 5;
    boolean hasOverlayContent() = 6;
    oneway void startScroll() = 7;
    oneway void onScroll(in float progress) = 8;
    oneway void endScroll() = 9;

}
/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.appwidget;

import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;

public interface IAppWidgetService extends android.os.IInterface {

    abstract class Stub extends android.os.Binder implements android.app.IServiceConnection {
        public static IAppWidgetService asInterface(android.os.IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }

    //
    // for AppWidgetHost
    //
    ParceledListSlice startListening(IAppWidgetHost host, String callingPackage, int hostId,
            int[] appWidgetIds);
    void stopListening(String callingPackage, int hostId);
    int allocateAppWidgetId(String callingPackage, int hostId);
    void deleteAppWidgetId(String callingPackage, int appWidgetId);
    void deleteHost(String packageName, int hostId);
    void deleteAllHosts();
    RemoteViews getAppWidgetViews(String callingPackage, int appWidgetId);
    int[] getAppWidgetIdsForHost(String callingPackage, int hostId);
    IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId,
            int intentFlags);
    IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId,
            int intentFlags, int userId);

    //
    // for AppWidgetManager
    //
    void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views);
    void updateAppWidgetOptions(String callingPackage, int appWidgetId, Bundle extras);
    Bundle getAppWidgetOptions(String callingPackage, int appWidgetId);
    void partiallyUpdateAppWidgetIds(String callingPackage, int[] appWidgetIds,
            RemoteViews views);
    void updateAppWidgetProvider(ComponentName provider, RemoteViews views);
    void updateAppWidgetProviderInfo(ComponentName provider, String metadataKey);
    void notifyAppWidgetViewDataChanged(String packageName, int[] appWidgetIds, int viewId);
    ParceledListSlice getInstalledProvidersForProfile(int categoryFilter, int profileId,
            String packageName);
    AppWidgetProviderInfo getAppWidgetInfo(String callingPackage, int appWidgetId);
    boolean hasBindAppWidgetPermission(String packageName, int userId);
    void setBindAppWidgetPermission(String packageName, int userId, boolean permission);
    boolean bindAppWidgetId(String callingPackage, int appWidgetId,
            int providerProfileId, ComponentName providerComponent, Bundle options);
    boolean bindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent,
            IApplicationThread caller, IBinder token, IServiceConnection connection, int flags);

    int[] getAppWidgetIds(ComponentName providerComponent);
    boolean isBoundWidgetPackage(String packageName, int userId);
    boolean requestPinAppWidget(String packageName, ComponentName providerComponent,
            Bundle extras, IntentSender resultIntent);
    boolean isRequestPinAppWidgetSupported();
    void noteAppWidgetTapped(String callingPackage, int appWidgetId);
}

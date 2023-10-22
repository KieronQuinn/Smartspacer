/*
 * Copyright (C) 2021 The Android Open Source Project
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
package android.service.smartspace;

import android.app.Service;
import android.app.smartspace.SmartspaceConfig;
import android.app.smartspace.SmartspaceSessionId;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.List;

/**
 * A service used to share the lifecycle of smartspace UI (open, close, interaction)
 * and also to return smartspace result on a query.
 *
 */
public abstract class SmartspaceService extends Service {

    /**
     * The {@link Intent} that must be declared as handled by the service.
     *
     * <p>The service must also require the {android.permission#MANAGE_SMARTSPACE}
     * permission.
     *
     * @hide
     */
    public static final String SERVICE_INTERFACE =
            "android.service.smartspace.SmartspaceService";

    @CallSuper
    @Override
    public void onCreate() {
        super.onCreate();
        throw new RuntimeException("Stub!");
    }

    @Override
    @NonNull
    public final IBinder onBind(@NonNull Intent intent) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Gets called when the client calls <code> SmartspaceManager#createSmartspaceSession </code>.
     */
    public abstract void onCreateSmartspaceSession(@NonNull SmartspaceConfig config,
                                                   @NonNull SmartspaceSessionId sessionId);

    /**
     * Gets called when the client calls <code> SmartspaceSession#notifySmartspaceEvent </code>.
     */
    @MainThread
    public abstract void notifySmartspaceEvent(@NonNull SmartspaceSessionId sessionId,
                                               @NonNull SmartspaceTargetEvent event);

    /**
     * Gets called when the client calls <code> SmartspaceSession#requestSmartspaceUpdate </code>.
     */
    @MainThread
    public abstract void onRequestSmartspaceUpdate(@NonNull SmartspaceSessionId sessionId);

    /**
     * Gets called when the client calls <code> SmartspaceManager#destroy() </code>.
     */
    public abstract void onDestroySmartspaceSession(@NonNull SmartspaceSessionId sessionId);

    /**
     * Used by the prediction factory to send back results the client app. The can be called
     * in response to {@link #onRequestSmartspaceUpdate(SmartspaceSessionId)} or proactively as
     * a result of changes in predictions.
     */
    public final void updateSmartspaceTargets(@NonNull SmartspaceSessionId sessionId,
                                              @NonNull List<SmartspaceTarget> targets) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Destroys a smartspace session.
     */
    @MainThread
    public abstract void onDestroy(@NonNull SmartspaceSessionId sessionId);
}
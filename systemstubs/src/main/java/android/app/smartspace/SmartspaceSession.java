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
package android.app.smartspace;

import android.app.smartspace.ISmartspaceCallback.Stub;
import android.content.Context;
import android.content.pm.ParceledListSlice;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Client API to share information about the Smartspace UI state and execute query.
 *
 * <p>
 * Usage: <pre> {@code
 *
 * class MyActivity {
 *    private SmartspaceSession mSmartspaceSession;
 *
 *    void onCreate() {
 *         mSmartspaceSession = mSmartspaceManager.createSmartspaceSession(smartspaceConfig)
 *         mSmartspaceSession.registerSmartspaceUpdates(...)
 *    }
 *
 *    void onStart() {
 *        mSmartspaceSession.requestSmartspaceUpdate()
 *    }
 *
 *    void onTouch(...) OR
 *    void onStateTransitionStarted(...) OR
 *    void onResume(...) OR
 *    void onStop(...) {
 *        mSmartspaceSession.notifyEvent(event);
 *    }
 *
 *    void onDestroy() {
 *        mSmartspaceSession.unregisterPredictionUpdates()
 *        mSmartspaceSession.close();
 *    }
 *
 * }</pre>
 *
 */
public final class SmartspaceSession implements AutoCloseable {

    /**
     * Creates a new Smartspace ui client.
     * <p>
     * The caller should call {@link SmartspaceSession#destroy()} to dispose the client once it
     * no longer used.
     *
     * @param context          the {@link Context} of the user of this {@link SmartspaceSession}.
     * @param smartspaceConfig the Smartspace context.
     */
    // b/177858121 Create weak reference child objects to not leak context.
    SmartspaceSession(@NonNull Context context, @NonNull SmartspaceConfig smartspaceConfig) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Notifies the Smartspace service of a Smartspace target event.
     *
     * @param event The {@link SmartspaceTargetEvent} that represents the Smartspace target event.
     */
    public void notifySmartspaceEvent(@NonNull SmartspaceTargetEvent event) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Requests the smartspace service for an update.
     */
    public void requestSmartspaceUpdate() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Requests the smartspace service provide continuous updates of smartspace cards via the
     * provided callback, until the given callback is unregistered.
     *
     * @param listenerExecutor The listener executor to use when firing the listener.
     * @param listener         The listener to be called when updates of Smartspace targets are
     *                         available.
     */
    public void addOnTargetsAvailableListener(@NonNull Executor listenerExecutor,
            @NonNull OnTargetsAvailableListener listener) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Requests the smartspace service to stop providing continuous updates to the provided
     * callback until the callback is re-registered.
     *
     * @param listener The callback to be unregistered.
     * @see {@link SmartspaceSession#addOnTargetsAvailableListener(Executor,
     * OnTargetsAvailableListener)}.
     */
    public void removeOnTargetsAvailableListener(@NonNull OnTargetsAvailableListener listener) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Destroys the client and unregisters the callback. Any method on this class after this call
     * will throw {@link IllegalStateException}.
     */
    private void destroy() {
        throw new RuntimeException("Stub!");
    }

    @Override
    protected void finalize() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void close() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Listener to receive smartspace targets from the service.
     */
    public interface OnTargetsAvailableListener {

        /**
         * Called when a new set of smartspace targets are available.
         *
         * @param targets Ranked list of smartspace targets.
         */
        void onTargetsAvailable(@NonNull List<SmartspaceTarget> targets);
    }

    static class CallbackWrapper extends Stub {

        private final Consumer<List<SmartspaceTarget>> mCallback;
        private final Executor mExecutor;

        CallbackWrapper(@NonNull Executor callbackExecutor,
                @NonNull Consumer<List<SmartspaceTarget>> callback) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public void onResult(ParceledListSlice result) {
            throw new RuntimeException("Stub!");
        }
    }
}
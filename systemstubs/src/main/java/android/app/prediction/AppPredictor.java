/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.app.prediction;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Class that represents an App Prediction client.
 *
 * <p>
 * Usage: <pre> {@code
 *
 * class MyActivity {
 *    private AppPredictor mClient
 *
 *    void onCreate() {
 *         mClient = new AppPredictor(...)
 *         mClient.registerPredictionUpdates(...)
 *    }
 *
 *    void onStart() {
 *        mClient.requestPredictionUpdate()
 *    }
 *
 *    void onClick(...) {
 *        mClient.notifyAppTargetEvent(...)
 *    }
 *
 *    void onDestroy() {
 *        mClient.unregisterPredictionUpdates()
 *        mClient.close()
 *    }
 *
 * }</pre>
 *
 * @hide
 */
public final class AppPredictor {

    /**
     * Creates a new Prediction client.
     * <p>
     * The caller should call {@link AppPredictor#destroy()} to dispose the client once it
     * no longer used.
     *
     * @param context The {@link Context} of the user of this {@link AppPredictor}.
     * @param predictionContext The prediction context.
     */
    AppPredictor(@NonNull Context context, @NonNull AppPredictionContext predictionContext) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Notifies the prediction service of an app target event.
     *
     * @param event The {@link AppTargetEvent} that represents the app target event.
     */
    public void notifyAppTargetEvent(@NonNull AppTargetEvent event) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Notifies the prediction service when the targets in a launch location are shown to the user.
     *
     * @param launchLocation The launch location where the targets are shown to the user.
     * @param targetIds List of {@link AppTargetId}s that are shown to the user.
     */
    public void notifyLaunchLocationShown(@NonNull String launchLocation,
            @NonNull List<AppTargetId> targetIds) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Requests the prediction service provide continuous updates of App predictions via the
     * provided callback, until the given callback is unregistered.
     *
     * @see Callback#onTargetsAvailable(List).
     *
     * @param callbackExecutor The callback executor to use when calling the callback.
     * @param callback The Callback to be called when updates of App predictions are available.
     */
    public void registerPredictionUpdates(@NonNull Executor callbackExecutor,
            @NonNull AppPredictor.Callback callback) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Requests the prediction service to stop providing continuous updates to the provided
     * callback until the callback is re-registered.
     *
     * @see {@link AppPredictor#registerPredictionUpdates(Executor, Callback)}.
     *
     * @param callback The callback to be unregistered.
     */
    public void unregisterPredictionUpdates(@NonNull AppPredictor.Callback callback) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Requests the prediction service to dispatch a new set of App predictions via the provided
     * callback.
     *
     * @see Callback#onTargetsAvailable(List).
     */
    public void requestPredictionUpdate() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns a new list of AppTargets sorted based on prediction rank or {@code null} if the
     * ranker is not available.
     *
     * @param targets List of app targets to be sorted.
     * @param callbackExecutor The callback executor to use when calling the callback.
     * @param callback The callback to return the sorted list of app targets.
     */
    @Nullable
    public void sortTargets(@NonNull List<AppTarget> targets,
            @NonNull Executor callbackExecutor, @NonNull Consumer<List<AppTarget>> callback) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Destroys the client and unregisters the callback. Any method on this class after this call
     * with throw {@link IllegalStateException}.
     */
    public void destroy() {
        throw new RuntimeException("Stub!");
    }

    @Override
    protected void finalize() throws Throwable {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the id of this prediction session.
     *
     * @hide
     */
    public AppPredictionSessionId getSessionId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Callback for receiving prediction updates.
     */
    public interface Callback {

        /**
         * Called when a new set of predicted app targets are available.
         * @param targets Sorted list of predicted targets.
         */
        void onTargetsAvailable(@NonNull List<AppTarget> targets);
    }
}
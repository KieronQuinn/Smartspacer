package com.kieronquinn.app.smartspacer.utils.task

import android.app.ActivityManager
import android.app.ActivityManagerHidden
import android.app.ITaskStackListener
import android.content.ComponentName
import android.os.IBinder
import android.window.TaskSnapshot

class TaskListener(private val onStackChanged: () -> Unit): ITaskStackListener.Stub() {

    override fun onTaskStackChanged() {
        onStackChanged()
    }

    override fun onActivityPinned(packageName: String?, userId: Int, taskId: Int, stackId: Int) {
        //No-op
    }

    override fun onActivityUnpinned() {
        //No-op
    }

    override fun onPinnedActivityRestartAttempt(clearedTask: Boolean) {
        //No-op
    }

    override fun onPinnedStackAnimationStarted() {
        //No-op
    }

    override fun onPinnedStackAnimationEnded() {
        //No-op
    }

    override fun onActivityForcedResizable(packageName: String?, taskId: Int, reason: Int) {
        //No-op
    }

    override fun onActivityDismissingDockedTask() {
        //No-op
    }

    override fun onActivityDismissingDockedStack() {
        //No-op
    }

    override fun onActivityLaunchOnSecondaryDisplayFailed(
        taskInfo: ActivityManager.RunningTaskInfo?,
        requestedDisplayId: Int
    ) {
        //No-op
    }

    override fun onActivityLaunchOnSecondaryDisplayRerouted(
        taskInfo: ActivityManager.RunningTaskInfo?,
        requestedDisplayId: Int
    ) {
        //No-op
    }

    override fun onTaskCreated(taskId: Int, componentName: ComponentName?) {
        //No-op
    }

    override fun onTaskRemoved(taskId: Int) {
        //No-op
    }

    override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo?) {
        //No-op
    }

    override fun onTaskDescriptionChanged(taskInfo: ActivityManager.RunningTaskInfo?) {
        //No-op
    }

    override fun onActivityRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {
        //No-op
    }

    override fun onTaskRemovalStarted(taskInfo: ActivityManager.RunningTaskInfo?) {
        //No-op
    }

    override fun onTaskProfileLocked(
        taskInfo: ActivityManager.RunningTaskInfo?,
        userId: Int
    ) {
        //No-op
    }

    override fun onTaskProfileLocked(taskId: Int, userId: Int) {
        //No-op
    }

    override fun onTaskSnapshotInvalidated(taskId: Int) {
        //No-op
    }

    override fun onTaskSnapshotChanged(taskId: Int, snapshot: ActivityManagerHidden.TaskSnapshot?) {
        //No-op
    }

    override fun onTaskSnapshotChanged(taskId: Int, snapshot: TaskSnapshot?) {
        //No-op
    }

    override fun onSizeCompatModeActivityChanged(displayId: Int, activityToken: IBinder?) {
        //No-op
    }

    override fun onBackPressedOnTaskRoot(taskInfo: ActivityManager.RunningTaskInfo?) {
        //No-op
    }

    override fun onTaskDisplayChanged(taskId: Int, newDisplayId: Int) {
        //No-op
    }

    override fun onActivityRestartAttempt(
        task: ActivityManager.RunningTaskInfo?,
        homeTaskVisible: Boolean,
        clearedTask: Boolean,
        wasVisible: Boolean
    ) {
        //No-op
    }

    override fun onSingleTaskDisplayDrawn(displayId: Int) {
        //No-op
    }

    override fun onSingleTaskDisplayEmpty(displayId: Int) {
        //No-op
    }

    override fun onRecentTaskListUpdated() {
        //No-op
    }

    override fun onRecentTaskListFrozenChanged(frozen: Boolean) {
        //No-op
    }

    override fun onTaskFocusChanged(taskId: Int, focused: Boolean) {
        //No-op
    }

    override fun onTaskRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {
        //No-op
    }

    override fun onActivityRotation(displayId: Int) {
        //No-op
    }

    override fun onTaskMovedToBack(taskInfo: ActivityManager.RunningTaskInfo?) {
        //No-op
    }

    override fun onLockTaskModeChanged(mode: Int) {
        //No-op
    }

    override fun onRecentTaskRemovedForAddTask(taskId: Int) {
        //No-op
    }

}
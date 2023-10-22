package com.kieronquinn.app.smartspacer;

interface ITaskObserver {

    void onTasksChanged(in List<String> packages);

}
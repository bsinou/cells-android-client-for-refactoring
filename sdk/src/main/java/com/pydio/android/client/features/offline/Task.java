package com.pydio.android.client.features.offline;

public interface Task {
    int checkForChanges();
    void start();
    void stop();
}

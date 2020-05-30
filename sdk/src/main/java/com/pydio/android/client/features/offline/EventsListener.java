package com.pydio.android.client.features.offline;

public interface EventsListener {
    void onNewChanges(String session, String workspace, int count);
    void onProcessingChange(String session, String workspace, String path);
    void onChangeProcessed(String session, String workspace, String path);
    void onChangeFailed(String session, String workspace, String path);
}

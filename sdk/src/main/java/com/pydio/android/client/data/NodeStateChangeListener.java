package com.pydio.android.client.data;

public interface NodeStateChangeListener {
    void onShareStateChanged(String path, int state);
    void onOfflineStateChanged(String path, int progress);
    void onDownloadStateChanged(String path, int progress);
}

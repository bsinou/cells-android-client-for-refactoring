package com.pydio.android.client.data.nodes;

import com.pydio.android.client.features.offline.EventsListener;
import com.pydio.sdk.core.model.FileNode;

public interface OfflineInfo {
    boolean isWatched(FileNode node);
    boolean isUnderAWatchedFolder(FileNode node);
    boolean hasOfflineVersion(FileNode node);
    String status(FileNode node);
    void setEventListener(EventsListener listener);
}

package com.pydio.android.client.data.nodes;

import com.pydio.sdk.core.model.Node;

import java.util.List;

public interface EventHandler {
    void onCreated(Node... nodes);

    void onDeleted(Node... nodes);

    void onUpdated(Node... nodes);

    void onRenamed(Node node, String newName);

    void onShared(Node node);

    void onUnShared(Node node);

    void onBookmarked(Node node);

    void onUnBookmarked(Node node);

    void onWatched(Node node);

    void onUnWatched(Node node);
}

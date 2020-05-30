package com.pydio.android.client.data.nodes;

import com.pydio.sdk.core.model.Node;

public class NodeEvent {
    public final static int Created = 1;
    public final static int Deleted = 2;
    public final static int ContentUpdated = 3;
    public final static int Renamed = 4;
    public final static int Shared = 5;
    public final static int Unshared = 6;
    public final static int Bookmarked = 7;
    public final static int UnBookmarked = 8;
    public final static int Watched = 9;
    public final static int UnWatched = 10;


    private Node node;
    private String nodeName;
    private int type;

    public NodeEvent(int type, Node node) {
        this.node = node;
        this.type = type;
    }

    public NodeEvent(int type, Node node, String newName) {
        this.node = node;
        this.type = type;
        this.nodeName = newName;
    }
}

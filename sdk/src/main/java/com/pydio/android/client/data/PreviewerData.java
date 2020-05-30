package com.pydio.android.client.data;

import com.pydio.sdk.core.model.Node;

import java.util.List;

public class PreviewerData {

    private List<Node> nodes;
    private int index;
    private Session session;

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public int getIndex() {
        return index;
    }

    public Session getSession() {
        return session;
    }
}

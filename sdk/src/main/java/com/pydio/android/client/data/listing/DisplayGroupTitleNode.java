package com.pydio.android.client.data.listing;

import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.model.ObjectNode;

public class DisplayGroupTitleNode extends ObjectNode {
    private String label;

    DisplayGroupTitleNode(String title) {
        label = title;
    }

    @Override
    public int type() {
        return Node.TYPE_LOCAL_NODE;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public String getProperty(String key) {
        return null;
    }
}

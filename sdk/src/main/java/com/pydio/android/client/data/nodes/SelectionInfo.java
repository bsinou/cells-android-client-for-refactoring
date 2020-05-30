package com.pydio.android.client.data.nodes;

import com.pydio.sdk.core.model.Node;

public interface SelectionInfo {
    boolean inSelectionMode();
    boolean isSelected(Node node);
    boolean allSelected();
}

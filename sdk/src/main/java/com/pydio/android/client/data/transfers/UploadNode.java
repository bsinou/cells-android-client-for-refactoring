package com.pydio.android.client.data.transfers;

import com.pydio.sdk.core.model.FileNode;


public class UploadNode extends FileNode {
    public static final int Type = 50;

    @Override
    public int type() {
        return Transfer.NodeType;
    }
}

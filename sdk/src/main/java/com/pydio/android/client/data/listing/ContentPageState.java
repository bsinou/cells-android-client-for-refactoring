package com.pydio.android.client.data.listing;

import android.content.Context;

import com.pydio.android.client.data.GUIContext;
import com.pydio.android.client.data.Display;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.gui.view.group.Metrics;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Node;

public class ContentPageState {
    public int mode;
    public Context activityContext;
    public GUIContext guiContext;
    public Session session;
    public Node node;
    public Metrics metrics;
    public Display.Info displayInfo;
    public Sorter<FileNode> sorter;
}

package com.pydio.android.client.data.events;

import com.pydio.sdk.core.model.Node;

import java.util.List;

public interface PollEventsListener {
    void onEvents(String sid, String ws, String directory, List<Node> delete, List<Node> updated, List<Node> added);
}

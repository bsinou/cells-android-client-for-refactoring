package com.pydio.android.client.data;

import com.pydio.android.client.data.images.ThumbLoader;
import com.pydio.android.client.data.nodes.OfflineInfo;
import com.pydio.android.client.data.nodes.SelectionInfo;
import com.pydio.sdk.core.model.Node;

import java.util.List;

public interface GUIContext {

    void setTitle(String title);

    void setTitle(int resTitle);

    void refreshActionBar();

    ThumbLoader thumbLoader();

    void onAuthenticationRequired(Runnable afterAuth);

    OfflineInfo getOfflineInfo();

    SelectionInfo getSelectionInfo();

    void sessionUpdated(Session newSession);
}

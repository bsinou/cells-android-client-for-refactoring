package com.pydio.cells.android.app;

import com.pydio.android.client.data.Application;
import com.pydio.android.client.gui.activities.ServerURL;

public class CellsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        loginClass = ServerURL.class;
        newServerClass = ServerURL.class;
    }
}

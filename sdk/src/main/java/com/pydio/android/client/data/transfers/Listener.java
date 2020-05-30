package com.pydio.android.client.data.transfers;

import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.model.Message;

public interface Listener {
    void onNew(String session, String ws, String dir, String name, int type, long size);
    void onProgress(String session, String ws, String dir, String name, int type, long progress, long size);
    void onError(String session, String ws, String dir, String name, int type, Error error);
    void onFinish(String session, String ws, String dir, String name, int type, Message msg);
}

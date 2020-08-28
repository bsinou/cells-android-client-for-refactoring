package com.pydio.android.client.data.wrap;

import com.pydio.sdk.core.common.errors.Error;

import java.io.IOException;

public class ServerNode {
    private com.pydio.sdk.core.model.ServerNode wrapped;

    public ServerNode() {
        this.wrapped = new com.pydio.sdk.core.model.ServerNode();
    }

    public void resolveURL(String address) throws IOException {
        Error error = this.wrapped.resolve(address);
        if (error == null) {
            return;
        }

        throw new IOException(error.toString());
    }

    public com.pydio.sdk.core.model.ServerNode getWrapped() {
        return this.wrapped;
    }
}

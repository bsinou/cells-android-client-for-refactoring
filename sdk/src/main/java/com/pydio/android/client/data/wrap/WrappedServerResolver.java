package com.pydio.android.client.data.wrap;


import com.pydio.sdk.core.service.ServerResolution;

import java.io.IOException;

public class WrappedServerResolver {

    private ServerResolver resolver;

    public WrappedServerResolver(ServerResolver resolver) {
        this.resolver = resolver;
    }

    public String resolve(String id, boolean refresh) throws IOException {
        return this.resolver.resolve(id, refresh);
    }

    public void register() {
        ServerResolution.register("pyd", this::resolve);
    }
}

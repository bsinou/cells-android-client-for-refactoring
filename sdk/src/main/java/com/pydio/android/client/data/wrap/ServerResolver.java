package com.pydio.android.client.data.wrap;

import java.io.IOException;

public interface ServerResolver {
    String resolve(String id, boolean refresh) throws IOException;
}

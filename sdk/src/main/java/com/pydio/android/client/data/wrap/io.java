package com.pydio.android.client.data.wrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class io {

    public static long pipeRead(InputStream in, OutputStream out) throws IOException {
        return com.pydio.sdk.core.utils.io.pipeRead(in, out);
    }
}

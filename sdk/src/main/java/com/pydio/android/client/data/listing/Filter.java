package com.pydio.android.client.data.listing;

import com.pydio.sdk.core.model.Node;

public interface Filter {
    boolean match(Node n);
}

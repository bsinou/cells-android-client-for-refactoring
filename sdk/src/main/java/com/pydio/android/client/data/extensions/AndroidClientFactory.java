package com.pydio.android.client.data.extensions;

import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.ClientFactory;
import com.pydio.sdk.core.model.ServerNode;

public class AndroidClientFactory extends ClientFactory {

    @Override
    public Client Client(ServerNode node) {
        return new AndroidCellsClient(node);
    }
}

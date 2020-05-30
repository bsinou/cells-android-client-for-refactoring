package com.pydio.android.client.accounts;

import android.content.Intent;

import java.io.IOException;

public interface AuthenticationEventHandler {

    void onError(String error, String description);

    void handleToken(String jwt) throws IOException;

    void startIntent(Intent intent);
}

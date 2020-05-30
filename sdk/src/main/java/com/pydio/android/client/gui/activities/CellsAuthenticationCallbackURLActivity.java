package com.pydio.android.client.gui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.pydio.android.client.accounts.Accounts;

public class CellsAuthenticationCallbackURLActivity extends PydioActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri == null) {
                finish();
                return;
            }

            Accounts.manager.handlerURL(uri);
            finish();
        }
    }
}

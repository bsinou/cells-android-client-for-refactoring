package com.pydio.android.client.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Connectivity;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.gui.components.ConfirmDialogComponent;
import com.pydio.android.client.gui.dialogs.models.DialogData;
import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.ClientFactory;
import com.pydio.sdk.core.common.callback.RegistryItemHandler;
import com.pydio.sdk.core.common.errors.Code;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.ServerNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class EnterpriseID  extends AppCompatActivity {

    public static final String ExtraURL = "url";

    private TextInputLayout textInputLayout;
    private LinearLayout rootView;
    private LinearLayout formLayout;
    private TextInputEditText urlEditText;
    private LinearLayout statusLayout;
    private TextView statusTextView;

    ServerNode server;

    private boolean processing;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_server_enterprise_id_layout);

        rootView = findViewById(R.id.root_view);

        Intent intent = getIntent();

        urlEditText = findViewById(R.id.url_edit_text);
        urlEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                resolveServer();
                return true;
            }
            return false;
        });
        String url = intent.getStringExtra(ExtraURL);
        if (url != null && url.length() > 0) {
            urlEditText.setText(url);
        }

        formLayout = findViewById(R.id.server_group);
        textInputLayout = findViewById(R.id.input_layout);

        Button actionButton = findViewById(R.id.action_button);
        actionButton.setOnClickListener((v) -> resolveServer());

        processing = false;
        initView();
    }

    private void initView() {
        initStatus();
    }

    private void initStatus() {
        statusLayout = findViewById(R.id.status_layout);
        statusTextView = findViewById(R.id.status_text);
        setStatusEditing();
    }

    @Override
    protected void onStart() {
        super.onStart();
        urlEditText.requestFocus();
    }

    private void resolveServer() {
        Connectivity con = Connectivity.get(this);
        if (!con.icConnected()) {
            showMessage(R.string.no_active_connection);
            return;
        }

        String address = urlEditText.getText().toString();
        if (address.length() == 0) {
            return;
        }

        textInputLayout.setEnabled(false);
        setStatusLoading();

        if (server == null) {
            server = new ServerNode();
        }

        String finalAddress = "pyd://" + address;
        Background.go(() -> {
            Error error = server.resolve(finalAddress);
            if (error != null) {
                setStatusEditing();

                if (error.code == Code.pydio_server_not_supported) {
                    showMessage(R.string.pydio_8_not_supported);

                } else if (error.code == Code.con_failed) {
                    showMessage(R.string.server_con_failed);

                } else if (error.code == Code.ssl_error) {
                    showMessage(R.string.server_ssl_error);

                } else if (error.code == Code.ssl_certificate_not_signed) {
                    showMessage(R.string.could_not_verify_cert);
                    byte[][] certs = server.getCertificateChain();
                    if (certs != null && certs.length > 0) {
                        CertificateFactory certFactory = null;
                        try {
                            certFactory = CertificateFactory.getInstance("X.509");
                            InputStream in = new ByteArrayInputStream(certs[0]);
                            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);
                            DialogData data = DialogData.acceptCertificate(EnterpriseID.this, cert, () -> {
                                Database.saveCertificate(server.url(), cert);
                                server.setUnverifiedSSL(true);
                            });
                            rootView.post(() -> {
                                ConfirmDialogComponent cdc = new ConfirmDialogComponent(EnterpriseID.this, data);
                                cdc.show();
                            });
                        } catch (CertificateException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    showMessage("failed to get server info");
                    // TODO: handle properly error code
                }
                return;
            }


            Client client = ClientFactory.get().Client(server);
            try {
                client.downloadServerRegistry(new RegistryItemHandler() {
                    @Override
                    public void onPref(String name, String value) {
                        super.onPref(name, value);
                        server.setProperty(name, value);
                    }
                });
            } catch (SDKException ignore) {}

            setStatusEditing();
            Session session = new Session();
            session.server = server;
            Application.addSession(session);
            Application.setPreference(Application.ENTERPRISE_ID, finalAddress.replaceFirst("pyd://", ""));
            goToLoginPage(session);
        });
    }

    private void setStatusLoading() {
        if (processing) {
            return;
        }
        processing = true;
        rootView.getHandler().post(() -> statusLayout.setVisibility(View.VISIBLE));
    }

    private void setStatusEditing() {
        processing = false;
        rootView.post(() -> statusLayout.setVisibility(View.INVISIBLE));
    }

    private void goToLoginPage(Session session) {
        rootView.post(() -> {
            Intent intent = new Intent(this, UserCredentials.class);
            intent.putExtra(UserCredentials.ExtraSessionID, session.id());
            intent.putExtra(UserCredentials.ExtraFromURLForm, true);
            EnterpriseID.this.startActivity(intent);
            EnterpriseID.this.finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void showMessage(String message) {
        rootView.post(() -> {
            textInputLayout.setError(message);
            textInputLayout.setEnabled(true);
        });
    }

    private void showMessage(int res) {
        rootView.post(() -> {
            textInputLayout.setError(getString(res));
            textInputLayout.setEnabled(true);
        });
    }
}

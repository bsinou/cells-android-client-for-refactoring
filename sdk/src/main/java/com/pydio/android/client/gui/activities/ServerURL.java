package com.pydio.android.client.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pydio.android.client.R;
import com.pydio.android.client.accounts.Accounts;
import com.pydio.android.client.accounts.AuthenticationEventHandler;
import com.pydio.sdk.core.auth.OauthConfig;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Connectivity;
import com.pydio.android.client.data.PydioAgent;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.State;
import com.pydio.sdk.core.auth.jwt.JWT;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.gui.components.ConfirmDialogComponent;
import com.pydio.android.client.gui.dialogs.models.DialogData;
import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.common.callback.RegistryItemHandler;
import com.pydio.sdk.core.common.errors.Code;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.ServerNode;
import com.pydio.sdk.core.auth.Token;
import com.pydio.sdk.core.model.WorkspaceNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ServerURL extends AppCompatActivity implements AuthenticationEventHandler {

    public static final String ExtraURL = "url";
    private TextInputLayout textInputLayout;
    private LinearLayout rootView;
    private TextInputEditText urlEditText;
    private LinearLayout statusLayout;

    ServerNode server;
    Session session;

    private boolean processing;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_url_layout);

        rootView = findViewById(R.id.root_view);

        Intent intent = getIntent();
        String url = intent.getStringExtra(ExtraURL);
        if (url == null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Bundle restrictions = Application.getRestrictions();
                if (restrictions != null && restrictions.containsKey("server_url")) {
                    url = restrictions.getString("server_url");
                }
            }
        }

        urlEditText = findViewById(R.id.url_edit_text);
        urlEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                resolveServer();
                return true;
            }
            return false;
        });
        if (url != null && url.length() > 0) {
            urlEditText.setText(url);
        }

        // formLayout = findViewById(R.id.server_group);
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
        // statusTextView = findViewById(R.id.status_text);
        setStatusEditing();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // urlEditText.setText("https://files.example.com");
        urlEditText.requestFocus();
    }

    private void resolveServer() {
        Connectivity con = Connectivity.get(this);
        if (!con.icConnected()) {
            showMessage(R.string.no_active_connection);
            return;
        }

        textInputLayout.setEnabled(false);
        Editable text = urlEditText.getText();
        if (text == null) {
            return;
        }

        String address = text.toString();
        try {
            new URL(address);
        } catch (MalformedURLException e) {
            showMessage(R.string.server_url_required);
            return;
        }

        setStatusLoading();

        if (server == null) {
            server = new ServerNode();
        }

        Background.go(() -> {
            Error error = server.resolve(address);
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
                            DialogData data = DialogData.acceptCertificate(ServerURL.this, cert, () -> {
                                Database.saveCertificate(server.url(), cert);
                                server.setUnverifiedSSL(true);
                            });
                            rootView.post(() -> {
                                ConfirmDialogComponent cdc = new ConfirmDialogComponent(ServerURL.this, data);
                                cdc.show();
                            });
                        } catch (CertificateException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    showMessage("failed to get server info");
                    //todo: handle properly error code
                }
                return;
            }


            Client client = Client.get(server);
            try {
                client.downloadServerRegistry(new RegistryItemHandler() {
                    @Override
                    public void onPref(String name, String value) {
                        super.onPref(name, value);
                        server.setProperty(name, value);
                    }
                });
            } catch (SDKException ignore) {
            }

            setStatusEditing();
            Session session = new Session();
            session.server = server;
            Application.addSession(session);
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
        if (this.server.supportsOauth()) {
            OauthConfig cfg = OauthConfig.fromJSON(this.server.getOIDCInfo(), "");
            this.session = session;
            Accounts.manager.authorize(cfg, this);
            return;
        }

        rootView.post(() -> {
            Intent intent = new Intent(this, UserCredentials.class);
            intent.putExtra(UserCredentials.ExtraSessionID, session.id());
            intent.putExtra(UserCredentials.ExtraFromURLForm, true);
            ServerURL.this.startActivity(intent);
            ServerURL.this.finish();
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

    @Override
    public void onError(String error, String description) {
        showMessage(error);
    }

    @Override
    public void handleToken(String stringToken) throws IOException {
        setStatusEditing();

        Token t;
        try {
            t = Token.decodeOauthJWT(stringToken);

        } catch (ParseException e) {
            e.printStackTrace();
            showMessage(R.string.could_not_get_token);
            return;
        }

        JWT jwt = JWT.parse(t.idToken);
        if (jwt == null) {
            showMessage(R.string.could_not_decode_id_token);
            return;
        }

        t.expiry = System.currentTimeMillis()/1000 + t.expiry;
        t.subject = String.format("%s@%s", jwt.claims.name, server.url());
        Database.saveToken(t);

        this.session.user = jwt.claims.name;
        this.session.server = this.server;

        PydioAgent agent = new PydioAgent(this.session);
        try {
            List<WorkspaceNode> workspaceNodes = new ArrayList<>();
            agent.client.workspaceList((node) -> workspaceNodes.add((WorkspaceNode) node));
            session.server.setWorkspaces(workspaceNodes);
            Application.saveSession(this.session);
        } catch (SDKException e) {
            e.printStackTrace();
        }

        State state = new State();
        state.setSaver((v, err) -> Application.setPreference(Application.PREF_APP_STATE, v));
        Application.saveSession(this.session);
        state.session = session.id();

        rootView.post(() -> {
            Intent intent = new Intent(ServerURL.this, Browser.class);
            intent.putExtra("state", state.toString());
            ServerURL.this.startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            ServerURL.this.finish();
        });
    }

    @Override
    public void startIntent(Intent intent) {
        startActivityForResult(intent, 0);
    }
}

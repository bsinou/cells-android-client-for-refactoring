package com.pydio.android.client.gui.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Connectivity;
import com.pydio.android.client.data.PydioAgent;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.State;
import com.pydio.android.client.data.Theme;
import com.pydio.android.client.data.auth.AppCredentials;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.gui.components.CaptchaInputDialog;
import com.pydio.android.client.gui.dialogs.models.CaptchaInputDialogData;
import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.api.p8.consts.Param;
import com.pydio.sdk.core.common.errors.Code;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.WorkspaceNode;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UserCredentials extends AppCompatActivity {
    public static final String ExtraSessionID = "sid";
    public static final String ExtraFromURLForm = "from_url_form";

    private LinearLayout rootView;

    private TextInputEditText loginInput;
    private TextInputLayout loginInputLayout;
    private String loginInputError;

    private TextInputEditText passwordInput;
    private TextInputLayout passwordInputLayout;
    private String passwordInputError;

    private LinearLayout statusLayout;

    Session session;
    private boolean processing;

    private boolean fromURLForm;

    private CaptchaInputDialog captchaDialog;
    private PydioAgent agent;
    private AppCredentials credentials;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        Intent intent = getIntent();
        if (!intent.hasExtra(ExtraSessionID)) {
            this.finish();
        }

        String sessionID = intent.getStringExtra(ExtraSessionID);
        session = Application.findSession(sessionID);
        if (session == null) {
            this.finish();
        }

        fromURLForm = intent.getBooleanExtra(ExtraFromURLForm, false);

        setContentView(R.layout.activity_user_crendentials_layout);

        LinearLayout actionBar = findViewById(R.id.action_bar);
        TextView welcomeMessageTextView = findViewById(R.id.welcome_message_text_view);

        rootView = findViewById(R.id.root_view);

        loginInput = findViewById(R.id.login_input);
        loginInputLayout = findViewById(R.id.login_input_layout);

        passwordInput = findViewById(R.id.password_input);
        passwordInputLayout = findViewById(R.id.password_input_layout);


        Button actionButton = findViewById(R.id.action_button);
        actionButton.setOnClickListener((v) -> connect());

        String welcomeMessage = session.server.welcomeMessage();
        if (welcomeMessage == null || "".equals(welcomeMessage)) {
            welcomeMessage = String.format(getString(R.string.default_welcome_message), session.server.label());
        }
        welcomeMessageTextView.setText(welcomeMessage);
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                connect();
                return true;
            }
            return false;
        });

        this.credentials = new AppCredentials(session.server.url());
        initView();

        if(Application.customTheme() != null) {
            actionButton.setTextColor(Application.customTheme().getMainColor());
            actionBar.setBackgroundColor(Application.customTheme().getMainColor());
            welcomeMessageTextView.setTextColor(Application.customTheme().getSecondaryColor());
            if (Build.VERSION.SDK_INT >= 21) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(Application.customTheme().getMainColor());
            }
        }
    }


    //***************************************************
    //                  Init
    //***************************************************
    private void initView() {
        initStatus();
    }

    private void initStatus() {
        statusLayout = findViewById(R.id.status_layout);
        setStatusEditing();
    }

    //***************************************************
    //                  Theme
    //***************************************************
    public static void applyTheme(EditText view) {
        final Theme theme = Application.customTheme();
        try {
            // Get the cursor resource id
            Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
            field.setAccessible(true);
            int drawableResId = field.getInt(view);

            // Get the editor
            field = TextView.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            Object editor = field.get(view);

            // Get the drawable and set a color filter
            Drawable drawable = ContextCompat.getDrawable(view.getContext(), drawableResId);
            if( drawable != null) {
                drawable.setColorFilter(theme.getMainColor(), PorterDuff.Mode.SRC_IN);
                Drawable[] drawables = {drawable, drawable};

                // Set the drawables
                field = editor.getClass().getDeclaredField("mCursorDrawable");
                field.setAccessible(true);
                field.set(editor, drawables);
            }

            ColorStateList colorStateList = ColorStateList.valueOf(theme.getMainColor());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setBackgroundTintList(colorStateList);
            }
        } catch (Exception ignored) {}
    }

    //***************************************************
    //                  Activity methods
    //***************************************************
    @Override
    protected void onStart() {
        super.onStart();
        loginInput.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (fromURLForm) {
            Intent intent = new Intent(this, Application.newServerClass);
            intent.putExtra(ServerURL.ExtraURL, session.server.url());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            this.finish();
        }
        super.onBackPressed();
    }

    //***************************************************
    //                  Pydio
    //***************************************************
    private void connect() {
        if (processing) {
            return;
        }

        Connectivity con = Connectivity.get(this);
        if (!con.icConnected()) {
            showMessage(R.string.no_active_connection);
            return;
        }

        setStatusLoading();

        Editable login = loginInput.getText();
        if (login == null || "".equals(login.toString())) {
            loginInputError = getString(R.string.field_required);
            onError();
            return;
        }

        Editable password = passwordInput.getText();
        if (password == null || "".equals(password.toString())) {
            passwordInputError = getString(R.string.field_required);
            onError();
            return;
        }

        loginInputLayout.setErrorEnabled(false);
        passwordInputLayout.setErrorEnabled(false);

        session.user = login.toString();
        this.credentials.setLogin(session.user);

        if (agent == null) {
            agent = new PydioAgent(session);
        }
        agent.client.setCredentials(this.credentials);

        Database.addPassword(session.idForCredentials(), password.toString());
        Background.go(() -> {
            if (this.credentials.getCaptcha() != null) {
                try {
                    agent.client.login();
                } catch (SDKException e) {
                    this.handleError(Error.fromException(e));
                    return;
                } finally {
                    this.credentials.setCaptcha(null);
                }
            }

            List<WorkspaceNode> workspaceNodes = new ArrayList<>();
            try {
                agent.client.workspaceList((node) -> workspaceNodes.add((WorkspaceNode) node));
                session.server.setWorkspaces(workspaceNodes);
                State state = new State();
                state.setSaver((v, err) -> Application.setPreference(Application.PREF_APP_STATE, v));
                Application.saveSession(session);
                state.session = session.id();
                rootView.post(() -> {
                    Intent intent = new Intent(UserCredentials.this, Browser.class);
                    intent.putExtra("state", state.toString());
                    UserCredentials.this.startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    UserCredentials.this.finish();
                });
            } catch (SDKException e) {
                this.handleError(Error.fromException(e));
            }
        });
    }

    void onCaptchaValue(String value) {
        this.credentials.setCaptcha(value);
    }

    void refreshCaptcha() {
        Background.go(() -> {
            try {
                InputStream in = this.agent.client.getCaptcha();
                rootView.post(() -> captchaDialog.setImage(in));
            } catch (SDKException e1) {
                e1.printStackTrace();
            }
        });
    }

    //***************************************************
    //                  Messages
    //***************************************************
    void handleError(Error e) {
        setStatusEditing();
        if (e.code == Code.authentication_required || e.code == Code.authentication_with_captcha_required) {
            showMessage(R.string.authentication_failed);
            try {
                JSONObject auth = this.agent.client.authenticationInfo();
                if (auth != null) {
                    this.credentials.setSeed(auth.getString(Param.seed));
                    if (auth.getBoolean(Param.captchaCode)) {
                        CaptchaInputDialogData data = CaptchaInputDialogData.create(UserCredentials.this, UserCredentials.this::onCaptchaValue, UserCredentials.this::refreshCaptcha);
                        rootView.post(() -> {
                            captchaDialog = new CaptchaInputDialog(UserCredentials.this, data);
                            captchaDialog.show();
                        });
                    }
                }
            } catch (SDKException e1) {
                e1.printStackTrace();
                showMessage(R.string.getting_authentication_info_failed);
            } catch (Exception e1) {
                e1.printStackTrace();
                showMessage(R.string.getting_authentication_info_failed);
            }
        } else if (e.code == Code.con_closed) {
            showMessage(R.string.server_con_failed);

        } else {
            //showMessage(e.text);
            //todo: handle properly error code
            showMessage(R.string.server_con_failed);
        }
    }

    void onError() {
        if (loginInputError != null && !"".equals(loginInputError)) {
            loginInputLayout.setError(loginInputError);
            loginInputLayout.setErrorEnabled(true);
        }

        if (passwordInputError != null && !"".equals(passwordInputError)) {
            passwordInputLayout.setError(passwordInputError);
            passwordInputLayout.setErrorEnabled(true);
        }
        setStatusEditing();
        loginInputError = "";
        passwordInputError = "";
    }

    void showMessage(int res) {
        showMessage(getString(res));
    }

    void showMessage(String text) {
        rootView.post(() -> {
            passwordInputLayout.setErrorEnabled(true);
            passwordInputLayout.setError(text);
        });
    }

    //***************************************************
    //                  Pydio
    //***************************************************
    private void setStatusLoading() {
        processing = true;
        rootView.post(() -> statusLayout.setVisibility(View.VISIBLE));
    }

    private void setStatusEditing() {
        processing = false;
        rootView.post(() -> statusLayout.setVisibility(View.INVISIBLE));
    }
}

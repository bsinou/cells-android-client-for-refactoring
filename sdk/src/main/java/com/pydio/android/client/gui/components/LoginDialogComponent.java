package com.pydio.android.client.gui.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.PydioAgent;
import com.pydio.android.client.data.Theme;
import com.pydio.android.client.data.auth.AppCredentials;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.api.p8.consts.Param;
import com.pydio.sdk.core.common.errors.SDKException;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Field;

public class LoginDialogComponent extends Component {

    private PydioAgent agent;
    private Context context;
    private AppCredentials credentials;
    private Runnable onLogin;

    private ImageView captchaImage;
    private TextInputEditText captchaEdit;
    private TextInputEditText passwordEdit;
    private TextInputLayout captchaLayout;

    private boolean captchaRequired;

    private AlertDialog dialog;

    public LoginDialogComponent(Context context, PydioAgent agent, Runnable onLogin) {
        this.context = context;
        this.agent = agent;
        this.credentials = new AppCredentials(agent.session.server.url());
        this.credentials.setLogin(agent.session.user);
        this.onLogin = onLogin;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initView();
        }
    }

    protected void initView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle(this.agent.session.user);
        builder.setIcon(R.drawable.lock);

        Drawable iconDrawable = context.getResources().getDrawable(R.drawable.lock);
        iconDrawable.mutate().setColorFilter(context.getResources().getColor(R.color.black4), PorterDuff.Mode.SRC_IN);
        builder.setIcon(iconDrawable);

        builder.setView(R.layout.dialog_login_layout);
        builder.setPositiveButton(R.string.login, (dialog, which) -> {
        });
        this.dialog = builder.create();
    }


    private void refresh() {
        Background.go(() -> {
            Handler h = new Handler(this.context.getMainLooper());
            try {
                JSONObject json = agent.client.authenticationInfo();
                this.captchaRequired = json.has(Param.captchaCode);
                if (this.captchaRequired) {
                    InputStream in = this.agent.client.getCaptcha();
                    Bitmap b = BitmapFactory.decodeStream(in);
                    h.post(() -> captchaImage.setImageBitmap(b));
                }
                this.credentials.setSeed(json.getString(Param.seed));
            } catch (SDKException e1) {
                e1.printStackTrace();
            } catch (Exception ignore) {
            }

            h.post(() -> {
                if (captchaRequired) {
                    captchaEdit.requestFocus();
                    captchaImage.setVisibility(View.VISIBLE);
                    captchaLayout.setVisibility(View.VISIBLE);
                } else {
                    passwordEdit.requestFocus();
                    captchaImage.setVisibility(View.GONE);
                    captchaLayout.setVisibility(View.GONE);
                }
                this.captchaEdit.setText("");
                this.passwordEdit.setText("");
            });
        });
    }

    @Override
    public void show() {
        Window w = dialog.getWindow();
        if (w != null) {
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        final LoginDialogComponent o = this;
        dialog.setOnShowListener(d -> {

            passwordEdit = this.dialog.findViewById(R.id.password_input);
            captchaEdit = this.dialog.findViewById(R.id.captcha_input);
            captchaImage = this.dialog.findViewById(R.id.captcha_image);
            captchaLayout = this.dialog.findViewById(R.id.captcha_edit_layout);
            Button button = LoginDialogComponent.this.dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (Application.customTheme() != null) {
                final Theme theme = Application.customTheme();
                button.setTextColor(theme.getMainColor());
                applyTheme(passwordEdit);
                applyTheme(captchaEdit);
            }
            button.setOnClickListener((dlg) -> {
                String password = o.passwordEdit.getText().toString();
                String captcha = o.captchaEdit.getText().toString();
                if (o.captchaRequired && "".equals(captcha) || "".equals(password)) {
                    return;
                }
                Database.addPassword(o.agent.session.idForCredentials(), password);
                if (o.captchaRequired) {
                    o.credentials.setCaptcha(captcha);
                }
                o.agent.client.setCredentials(o.credentials);
                Background.go(() -> {
                    try {
                        o.agent.client.login();
                        o.onLogin.run();
                        o.dialog.dismiss();
                    } catch (SDKException e) {
                        e.printStackTrace();
                        try {
                            JSONObject json = agent.client.authenticationInfo();
                            json.has(Param.captchaCode);
                            Handler h = new Handler(o.context.getMainLooper());
                            h.post(() -> {
                                o.captchaEdit.setText("");
                                o.passwordEdit.setText("");
                            });
                        } catch (SDKException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            });
            refresh();
        });
        dialog.show();
    }

    @Override
    public void hide() {
        dialog.dismiss();
    }

    @Override
    public View getView() {
        return null;
    }

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
}

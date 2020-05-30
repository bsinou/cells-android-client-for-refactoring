package com.pydio.android.client.gui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;

import com.pydio.android.client.R;
import com.pydio.android.client.gui.dialogs.models.CaptchaInputDialogData;
import com.pydio.android.client.utils.Background;

import java.io.InputStream;

public class CaptchaInputDialog extends Component {

    private AlertDialog dialog;
    private CaptchaInputDialogData data;
    private Context context;
    private ImageView imageView;
    private ProgressBar progressBar;

    public CaptchaInputDialog(Context context, CaptchaInputDialogData data) {
        this.data = data;
        this.context = context;
        if (data != null) {
            populate();
        }
    }

    private void populate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setIcon(data.iconRes).setTitle(data.title);
        builder.setTitle(this.data.title);
        builder.setView(R.layout.dailog_captcha_input_layout);
        builder.setPositiveButton(this.context.getString(R.string.submit), (dialog, which) -> {
            EditText input = this.dialog.findViewById(R.id.input);
            if (input != null) {
                CaptchaInputDialog.this.data.action.onComplete(input.getText().toString());
            }
            dialog.dismiss();
        });
        builder.setNeutralButton(R.string.refresh, (d, v) -> this.data.requireRefresh.run());
        this.dialog = builder.create();
        this.dialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener((di) -> {
                progressBar.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);
                this.data.requireRefresh.run();
            });
            this.imageView = this.dialog.findViewById(R.id.captcha_image);
            this.progressBar = this.dialog.findViewById(R.id.progress_bar);
            data.requireRefresh.run();
        });
    }

    public void setImage(InputStream in) {
        progressBar.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        Background.go(() -> {
            final Bitmap bitmap = BitmapFactory.decodeStream(in);
            this.imageView.post(() -> {
                this.imageView.setImageBitmap(bitmap);
            });
        });
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void show() {
        Window w = dialog.getWindow();
        if (w != null) {
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
    }

    @Override
    public void hide() {
        dialog.dismiss();
    }
}

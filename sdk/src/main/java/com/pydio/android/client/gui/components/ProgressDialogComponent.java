package com.pydio.android.client.gui.components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Theme;
import com.pydio.android.client.gui.dialogs.models.ProgressDialogData;

public class ProgressDialogComponent extends Component {

    private AlertDialog dialog;
    private Context context;
    private ProgressDialogData data;

    private ProgressBar progressBar;
    private TextView textView;
    private Handler handler;

    private Runnable onCancel;

    public ProgressDialogComponent(Context context, ProgressDialogData data) {
        this.context = context;
        this.handler = new Handler(context.getMainLooper());
        this.data = data;
        populate();
    }

    public void update(String message, boolean indeterminate, int progress) {
        this.handler.post(() -> {
            if (getProgressBar() != null) {
                this.progressBar.setIndeterminate(indeterminate);
                if (!indeterminate) {
                    this.progressBar.setProgress(progress);
                }
            }


            if (message != null) {
                if (getTextView() != null) {
                    this.textView.setText(message);
                }
            }
        });
    }

    private void populate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setIcon(data.iconRes).setTitle(data.title).setMessage(data.text);
        builder.setTitle(this.data.title);
        builder.setView(R.layout.dialog_transfer_layout);
        //builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        this.dialog = builder.create();
        if(Application.customTheme() != null) {
            final Theme theme = Application.customTheme();
            this.dialog.setOnShowListener((d) -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(theme.getMainColor());
                ProgressBar progressBar = this.dialog.findViewById(R.id.progress);
                if(progressBar != null) {
                    LayerDrawable progressBarDrawable = (LayerDrawable) progressBar.getProgressDrawable();
                    Drawable backgroundDrawable = progressBarDrawable.getDrawable(0);
                    Drawable progressDrawable = progressBarDrawable.getDrawable(1);

                    backgroundDrawable.setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_IN);
                    progressDrawable.setColorFilter(theme.getMainColor(), PorterDuff.Mode.SRC_IN);
                }
            });
        }
        this.dialog.setOnDismissListener(dialog -> {
            if (this.onCancel != null) {
                this.onCancel.run();
            }
        });
        Window w = this.dialog.getWindow();
        if(w != null){
            w.setLayout(300, 100);
        }
    }

    private ProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = this.dialog.findViewById(R.id.progress);
        }
        return progressBar;
    }

    private TextView getTextView() {
        if (textView == null) {
            textView = this.dialog.findViewById(R.id.text);
        }
        return textView;
    }

    public void onCancelListener(Runnable c) {
        this.onCancel = c;
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void show() {
        this.dialog.show();
    }

    @Override
    public void hide() {
        this.dialog.dismiss();
    }
}

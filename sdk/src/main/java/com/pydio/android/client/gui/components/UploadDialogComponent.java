package com.pydio.android.client.gui.components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Theme;
import com.pydio.android.client.gui.dialogs.models.UploadDialogData;

public class UploadDialogComponent extends Component {

    private AlertDialog dialog;
    private ProgressBar progress;
    private TextView textView;

    private Context context;
    private Runnable cancelAction;

    public UploadDialogComponent(Context context) {
        this.context = context;
        initView();
    }

    public void update(UploadDialogData data) {
        if (progress == null) {
            return;
        }

        dialog.setIcon(R.drawable.upload);
        dialog.setTitle(R.string.upload);
        textView.setText(data.name);

        if (data.progress > 0) {
            progress.setIndeterminate(false);
        } else {
            progress.setProgress(data.progress);
        }
        show();
    }

    protected void initView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setIcon(R.drawable.upload).setTitle(R.string.upload);
        builder.setView(R.layout.dialog_upload_layout);
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> onCancel());
        this.dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> onCancel());
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
        Window w = this.dialog.getWindow();
        if (w != null) {
            w.setLayout(300, 100);
        }
        progress = dialog.findViewById(R.id.progress);
    }

    private void onCancel() {
        dialog.hide();
        if (this.cancelAction != null) {
            this.cancelAction.run();
        }
    }

    public void setCancelAction(Runnable r) {
        this.cancelAction = r;
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void show() {
        if (dialog.isShowing()) {
            return;
        }
        dialog.setOnShowListener(dialogInterface -> {
            if (progress == null) {
                progress = dialog.findViewById(R.id.progress);
                textView = dialog.findViewById(R.id.text);
                if (progress != null) {
                    if (progress.getProgress() == 0) {
                        progress.setIndeterminate(true);
                    }
                }
            }
        });
        dialog.show();
    }

    @Override
    public void hide() {
        dialog.hide();
    }
}

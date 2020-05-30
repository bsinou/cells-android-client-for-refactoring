package com.pydio.android.client.gui.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Theme;
import com.pydio.android.client.gui.dialogs.models.TaskDialogData;

public class TaskDialogComponent extends Component {

    private AlertDialog dialog;
    private ProgressBar progress;
    private TextView textView;

    private Context context;
    private Runnable cancelAction;

    private boolean isShowing;

    TaskDialogData data;

    public TaskDialogComponent(Context context) {
        this.context = context;
        initView();
    }

    public void update(TaskDialogData data) {
        this.data = data;
        if (progress != null) {
            applyData();
        }
        show();
    }

    private void applyData() {
        if (data == null || progress == null) {
            return;
        }

        if (data.icon == 0) {
            dialog.setIcon(0);
        } else {
            dialog.setIcon(data.icon);
        }

        if (data.title == null || "".equals(data.title)) {
            dialog.setTitle("");
        } else {
            dialog.setTitle(data.title);
        }

        textView.setText(data.name);

        if (data.indeterminate) {
            progress.setIndeterminate(true);
        } else {
            progress.setIndeterminate(false);
            progress.setProgress(data.progress);
        }
    }

    public void initView() {
        isShowing = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setIcon(R.drawable.upload).setTitle(R.string.upload);
        builder.setView(R.layout.dialog_upload_layout);
        //builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> onCancel());
        this.dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> onCancel());
        dialog.setOnDismissListener(dialog -> {
        });
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
        if (isShowing) {
            return;
        }
        isShowing = true;
        dialog.setOnShowListener(dialogInterface -> {
            if (progress == null) {
                progress = dialog.findViewById(R.id.progress);
                textView = dialog.findViewById(R.id.text);
                if (progress != null) {
                    if (progress.getProgress() == 0) {
                        progress.setIndeterminate(true);
                    }
                }

                if(Application.customTheme() != null) {
                    final Theme theme = Application.customTheme();
                    ColorStateList colorStateList = ColorStateList.valueOf(theme.getMainColor());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        progress.setProgressTintList(colorStateList);
                        progress.setIndeterminateTintList(colorStateList);
                    }
                }
                applyData();
            }
        });
        dialog.show();
    }

    @Override
    public void hide() {
        dialog.dismiss();
        isShowing = false;
    }
}

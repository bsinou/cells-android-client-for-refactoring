package com.pydio.android.client.gui.components;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Resources;
import com.pydio.android.client.data.Theme;
import com.pydio.android.client.gui.dialogs.models.DialogData;

public class ConfirmDialogComponent extends Component {

    private AlertDialog dialog;
    private DialogData data;
    private Context context;

    public ConfirmDialogComponent(Context context, DialogData data){
        this.data = data;
        this.context = context;
        populate();
    }

    private void populate(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setTitle(data.title).setMessage(data.message);
        builder = builder.setIcon(Resources.drawable(context, data.iconRes, R.color.black4));
        builder.setPositiveButton(data.positiveText, (dialog, which) -> data.action.run());
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        this.dialog = builder.create();
        if (Application.customTheme() != null) {
            final Theme theme = Application.customTheme();
            this.dialog.setOnShowListener((d) -> {
                /*Drawable iconDrawable = activityContext.getResources().getDrawable(data.iconRes);
                iconDrawable.mutate().setColorFilter(customTheme.getMainColor(), PorterDuff.Mode.SRC_IN);
                dialog.setIcon(iconDrawable);*/
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.getMainColor());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(theme.getMainColor());
            });
        }
    }

    private void refresh(DialogData data){
        this.data = data;
        populate();
    }

    @Override
    public View getView() {
        return null;
    }
    @Override
    public void show() {
        dialog.show();
    }
    @Override
    public void hide() {
        dialog.dismiss();
    }
}

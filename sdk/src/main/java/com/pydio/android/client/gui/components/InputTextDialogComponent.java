package com.pydio.android.client.gui.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Theme;
import com.pydio.android.client.gui.dialogs.models.InputDialogData;

import java.lang.reflect.Field;


public class InputTextDialogComponent extends Component {

    private AlertDialog dialog;
    private InputDialogData data;
    private Context context;

    public InputTextDialogComponent(Context context, InputDialogData data) {
        this.data = data;
        this.context = context;
        if(data != null){
            populate();
        }
    }

    private void populate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setIcon(data.iconRes).setTitle(data.title).setMessage(data.text);
        builder.setTitle(this.data.title);
        builder.setView(R.layout.dailog_input_layout);

        if(data.positiveText == null || data.positiveText.length() == 0) {
            data.positiveText = this.context.getString(android.R.string.yes);
        }

        builder.setPositiveButton(data.positiveText, (dialog, which) -> {
            EditText input = this.dialog.findViewById(R.id.input);
            if(input != null){
                InputTextDialogComponent.this.data.action.onComplete(input.getText().toString());
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        this.dialog = builder.create();
    }

    public void update(InputDialogData data) {
        this.data = data;
        populate();
    }
    @Override
    public View getView() {
        return null;
    }
    @Override
    public void show() {
        Window w = dialog.getWindow();
        if(w != null) {
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        dialog.setOnShowListener(dialog -> {
            if (Application.customTheme() != null) {
                final Theme theme = Application.customTheme();
                this.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.getMainColor());
                this.dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(theme.getMainColor());
            }

            EditText input = this.dialog.findViewById(R.id.input);
            if(input != null){
                if (Application.customTheme() != null) {
                    applyTheme(input);
                }

                if(data.password){
                    input.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
                } else {
                    input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                }

                if(data.text != null) {
                    if(data.selected) {
                        input.setText(data.text);
                        int dotIndex = data.text.lastIndexOf('.');
                        if(dotIndex == -1) {
                            dotIndex = data.text.length();
                        }
                        input.setSelection(0, dotIndex);
                    }
                }
                input.requestFocus();
            }
        });
        dialog.show();
    }
    @Override
    public void hide() {
        dialog.dismiss();
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

package com.pydio.android.client.gui.dialogs.models;

import android.content.Context;

import com.pydio.android.client.R;
import com.pydio.android.client.data.callback.Completion;

public class InputDialogData {

    public Completion<String> action;
    public int iconRes;
    public String title;
    public String message;
    public String text;
    public boolean password;
    public boolean selected;
    public String positiveText;

    public static InputDialogData createFile(Context context, boolean folder, Completion<String> c){
        InputDialogData data = new InputDialogData();

        if(folder){
            data.title = context.getString(R.string.new_folder);
        } else {
            data.title = context.getString(R.string.create_file);
        }

        data.positiveText = context.getString(R.string.create);
        data.action = c;
        return data;
    }

    public static InputDialogData rename(Context context, String text, boolean selected, Completion<String> c){
        InputDialogData data = new InputDialogData();
        data.text = text;
        data.selected = selected;
        data.title = context.getString(R.string.rename);
        data.positiveText = context.getString(R.string.rename);
        data.action = c;
        return data;
    }

    public static InputDialogData password(Context context, String message, String actionText, Completion<String> c){
        InputDialogData data = new InputDialogData();
        data.message = message;
        data.selected = false;
        data.title = context.getString(R.string.password_hint);
        data.password = true;
        data.positiveText = actionText;
        data.action = c;
        return data;
    }
}

package com.pydio.android.client.gui.dialogs.models;

import android.content.Context;

import com.pydio.android.client.R;

public class ProgressDialogData {
    public String title;
    public String text;
    public boolean indeterminate;
    public int iconRes;


    public static ProgressDialogData download(Context context, String name){
        ProgressDialogData data = new ProgressDialogData();
        data.title = context.getString(R.string.download);
        data.indeterminate = true;
        data.text = context.getString(R.string.preparing);
        return data;
    }
}

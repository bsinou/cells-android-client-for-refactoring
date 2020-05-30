package com.pydio.android.client.gui.dialogs.models;

import android.content.Context;

import com.pydio.android.client.R;
import com.pydio.android.client.data.callback.Completion;

public class CaptchaInputDialogData {
    public Completion<String> action;
    public Runnable requireRefresh;
    public int iconRes;
    public String title;

    public static CaptchaInputDialogData create(Context context, Completion<String> completion, Runnable requireRefresh) {
        CaptchaInputDialogData data = new CaptchaInputDialogData();
        data.title = context.getString(R.string.captcha_hint);
        data.action = completion;
        data.requireRefresh = requireRefresh;
        return data;
    }
}

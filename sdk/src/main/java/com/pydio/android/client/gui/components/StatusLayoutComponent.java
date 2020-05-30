package com.pydio.android.client.gui.components;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pydio.android.client.R;

public class StatusLayoutComponent {
    private View rootView;
    private ProgressBar mProgress;
    private TextView mText;
    Runnable afterHideAction, afterShowAction;

    public StatusLayoutComponent(View rootView) {
        this.rootView = rootView;
        mText = rootView.findViewById(R.id.status_text);
        mProgress = rootView.findViewById(R.id.status_progress_bar);
    }

    public void setText(String text){
        mText.setText(text);
    }

    public void setText(int textRes){
        mText.setText(textRes);
    }

    public void setProgress(int progress) {
        mProgress.setProgress(progress);
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        mProgress.setIndeterminate(indeterminate);
    }

    public void show(){
        rootView.setVisibility(View.VISIBLE);
        if(afterShowAction != null) {
            afterShowAction.run();
        }
    }

    public void hide(){
        rootView.setVisibility(View.GONE);
        if(afterHideAction != null){
            afterHideAction.run();
        }
    }

    public void setOnShowListener(Runnable a){
        afterShowAction = a;
    }

    public void setOnHideListener(Runnable a){
        afterHideAction = a;
    }
}

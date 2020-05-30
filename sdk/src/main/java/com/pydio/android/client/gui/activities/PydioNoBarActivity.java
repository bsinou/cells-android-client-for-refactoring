package com.pydio.android.client.gui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;

public class PydioNoBarActivity extends AppCompatActivity {

    protected boolean mStartedActivityForResult = false;
    protected FrameLayout rootView;
    protected boolean mSetContentViewCalled;
    protected FloatingActionButton fab;

    Handler mHandler;
    Toast mToast;

    protected int rootLayout = -1;

    //**********************************************************************************************
    //                      Activity methods
    //**********************************************************************************************
    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            int darkBackgroundColor = com.pydio.android.client.data.Resources.darkBackgroundColor();
            if( darkBackgroundColor == -1) {
                window.setStatusBarColor(getResources().getColor(R.color.status_bar_color));
            } else {
                window.setStatusBarColor(darkBackgroundColor);
            }
        }

        mHandler = new Handler(getMainLooper());
        mToast = new Toast(this);

        mSetContentViewCalled = false;
    }

    @Override
    public void setContentView(int layoutResID) {
        if (mSetContentViewCalled) {
            return;
        }
        initView();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View v = inflater.inflate(layoutResID, null);
            rootView.addView(v);
        }
        mSetContentViewCalled = true;
    }
    @Override
    public void setContentView(View view) {
        if (mSetContentViewCalled) {
            return;
        }
        initView();
        rootView.addView(view);
        mSetContentViewCalled = true;
    }
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mSetContentViewCalled) {
            return;
        }
        initView();
        rootView.addView(view, params);
        mSetContentViewCalled = true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        Application.wasInBackground();
        if(mStartedActivityForResult){
            mStartedActivityForResult = false;
        }
    }
    @Override
    protected void onDestroy() {
        Application.stopActivityTransitionTimer();
        super.onDestroy();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        mStartedActivityForResult = true;
        super.startActivityForResult(intent, requestCode, options);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    public Handler getHandler() {
        return mHandler;
    }

    //**********************************************************************************************
    //                      INIT
    //**********************************************************************************************
    private void initView() {
        if(rootLayout == -1){
            rootLayout = R.layout.activity_pydio_no_bar;
        }

        super.setContentView(rootLayout);

        rootView = findViewById(R.id.pydio_activity_root);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(this::onFabClicked);

        if (Application.customTheme() != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageTintList(ColorStateList.valueOf(Application.customTheme().getSecondaryColor()));
            }
        }
    }

    //**********************************************************************************************
    //                      MESSAGES
    //**********************************************************************************************
    public synchronized void showMessage(String message) {
        new Handler(getMainLooper()).post(() -> {
                    Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
        );
    }

    public synchronized void showMessage(int res) {

        new Handler(getMainLooper()).post(() -> {
            final String message = getResources().getString(res);
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }

    public synchronized void showMessage(String format, Object... params) {
        new Handler(getMainLooper()).post(() -> {
            final String message = String.format(format, params);
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }

    public synchronized void showMessage(int resFormat, Object... params) {
        new Handler(getMainLooper()).post(() -> {
            final String message = String.format(getString(resFormat), params);
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
            snackbar.show();
        });
    }


    //**********************************************
    //          FAB
    //**********************************************
    protected void onFabClicked(View fab){

    }

    protected void showFAB(){
        fab.show();
    }

    protected void hideFAB(){
        fab.hide();
    }


    //**********************************************
    //          STATE
    //**********************************************
    public void hideKeyBoard(View v){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public void showKeyBoard(View v){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        }
    }
}

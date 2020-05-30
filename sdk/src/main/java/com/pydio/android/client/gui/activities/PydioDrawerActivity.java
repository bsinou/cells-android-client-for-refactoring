package com.pydio.android.client.gui.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.gui.components.ActionBarComponent;
import com.pydio.android.client.gui.menu.models.ActionData;

import java.util.ArrayList;
import java.util.List;

public class PydioDrawerActivity extends AppCompatActivity implements DrawerLayout.DrawerListener {
    protected NavigationView dataNavigationView, sessionsNavigationView;
    protected DrawerLayout drawerLayout;

    protected boolean mStartedActivityForResult = false;

    protected FrameLayout contentView;
    protected ActionBarComponent actionBarComponent;

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
            if (Application.customTheme() != null) {
                window.setStatusBarColor(Application.customTheme().getMainColor());
            } else {
                int darkBackgroundColor = com.pydio.android.client.data.Resources.darkBackgroundColor();
                if( darkBackgroundColor == -1) {
                    window.setStatusBarColor(getResources().getColor(R.color.status_bar_color));
                } else {
                    window.setStatusBarColor(darkBackgroundColor);
                }
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
            contentView.addView(v);
        }
        mSetContentViewCalled = true;
    }
    @Override
    public void setContentView(View view) {
        if (mSetContentViewCalled) {
            return;
        }
        initView();
        contentView.addView(view);
        mSetContentViewCalled = true;
    }
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mSetContentViewCalled) {
            return;
        }
        initView();
        contentView.addView(view, params);
        mSetContentViewCalled = true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        Application.wasInBackground();
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return true;
    }

    public Handler getHandler() {
        return mHandler;
    }

    //**********************************************************************************************
    //                      INIT
    //**********************************************************************************************
    private void initView() {
        super.setContentView(R.layout.activity_drawer_layout);

        rootLayout = R.layout.activity_drawer_layout;

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(this);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(this::onFabClicked);
        if (Application.customTheme() != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageTintList(ColorStateList.valueOf(Application.customTheme().getSecondaryColor()));
            }
        }

        contentView = findViewById(R.id.layout_content);
        dataNavigationView = findViewById(R.id.drawer_workspace_navigation_view);
        sessionsNavigationView = findViewById(R.id.drawer_sessions_navigation_view);
        initActionBar();
    }

    @SuppressLint("RestrictedApi")
    private void initActionBar() {
        View customActionBarContent = findViewById(R.id.action_bar_root_view);
        actionBarComponent = new ActionBarComponent(this, customActionBarContent);
        actionBarComponent.onHomeButtonClicked(this::homeButtonClicked);
    }

    //**********************************************************************************************
    //                      MESSAGES
    //**********************************************************************************************
    public synchronized void showMessage(String message) {
        new Handler(getMainLooper()).post(() -> {
                    Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
        );
    }

    public synchronized void showMessage(int res) {
        new Handler(getMainLooper()).post(() -> {
            final String message = getResources().getString(res);
            Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }

    public synchronized void showMessage(String format, Object... params) {
        new Handler(getMainLooper()).post(() -> {
            final String message = String.format(format, params);
            Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_SHORT);
            snackbar.show();
        });
    }

    public synchronized void showMessage(int resFormat, Object... params) {
        new Handler(getMainLooper()).post(() -> {
            final String message = String.format(getString(resFormat), params);
            Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_SHORT);
            snackbar.show();
        });
    }

    public synchronized void longShowMessage(String message) {
        new Handler(getMainLooper()).post(() -> {
                    Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
        );
    }

    public synchronized void longShowMessage(int res) {
        new Handler(getMainLooper()).post(() -> {
            final String message = getResources().getString(res);
            Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }

    public synchronized void longShowMessage(String format, Object... params) {
        new Handler(getMainLooper()).post(() -> {
            final String message = String.format(format, params);
            Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }


    //          ACTION BAR
    public void homeButtonClicked(View v){
        openLeftPanel();
    }

    public void setActionBarTitle(String title) {
        actionBarComponent.setTitle(title);
    }

    public void setActionBarHomeIcon(int resIcon) {
        actionBarComponent.setHomeIcon(resIcon);
    }

    protected boolean actionBarHomeButtonClicked(){
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long id = item.getItemId();
        if (id == android.R.id.home){
            return actionBarHomeButtonClicked();
        }
        return false;
    }

    protected List<ActionData> actionBarItemList(){
        return new ArrayList<>();
    }

    void refreshActionBar() {
        List<ActionData> actions = actionBarItemList();
        actionBarComponent.updateMenu(actions);
    }

    //          FAB
    protected void onFabClicked(View fab){

    }

    protected void showFAB(){
        fab.show();
    }

    protected void hideFAB(){
        fab.hide();
    }


    //          DRAWER LAYOUT
    protected void openLeftPanel() {
        drawerLayout.openDrawer(Gravity.START);
    }

    protected void closeLeftPanel() {
        drawerLayout.closeDrawer(Gravity.START);
    }

    protected void onLeftPanelOpened() {

    }

    protected void onLeftPanelClosed() {
        if(sessionsNavigationView.getVisibility() == View.VISIBLE){
            sessionsNavigationView.setVisibility(View.GONE);
        }
        dataNavigationView.setVisibility(View.VISIBLE);
    }


    //          ACTION
    @Override
    public void onDrawerSlide(@NonNull View view, float v) {

    }

    @Override
    public void onDrawerOpened(@NonNull View view) {
        onLeftPanelOpened();
    }

    @Override
    public void onDrawerClosed(@NonNull View view) {
        onLeftPanelClosed();
    }

    @Override
    public void onDrawerStateChanged(int i) {

    }

    //          KEYBOARD
    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }

        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}

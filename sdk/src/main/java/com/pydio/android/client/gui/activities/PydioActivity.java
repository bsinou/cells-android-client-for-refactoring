package com.pydio.android.client.gui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.gui.menu.models.ActionData;
import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.utils.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static com.amazonaws.util.IOUtils.copy;

public class PydioActivity extends AppCompatActivity {

    protected boolean mStartedActivityForResult = false;
    protected FrameLayout contentView;
    protected ActionBar mActionBar;

    protected boolean mSetContentViewCalled;
    protected FloatingActionButton fab;

    private LinearLayout popupMenu;
    private RelativeLayout popup;
    private View popupAnchor;
    private float popupHeight;

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
    protected void onStart() {
        super.onStart();
        Application.wasInBackground();
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
            rootLayout = R.layout.activity_pydio_layout;
        }

        super.setContentView(rootLayout);

        contentView = findViewById(R.id.layout_content);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(this::onFabClicked);
        if (Application.customTheme() != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageTintList(ColorStateList.valueOf(Application.customTheme().getSecondaryColor()));
            }
        }

        initPopup();
        initSearch();
        initActionBar();
    }

    private void initPopup() {
        popup = findViewById(R.id.menu_page);
        popupMenu = findViewById(R.id.popup_menu);
    }

    @SuppressLint("RestrictedApi")
    private void initActionBar() {
        //Toolbar toolBar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolBar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDefaultDisplayHomeAsUpEnabled(true);
            mActionBar.setElevation(2);
        }
    }

    private void initSearch() {

        /*private RelativeLayout mSearchPage;
        private RelativeLayout mSearchPanel;
        private LinearLayout mSearchFormLayout;
        private ImageView mSearchIcon;
        private LinearLayout mSearchTextLayout;
        private TextView mSearchLabelTextView;
        private AutoCompleteTextView mSearchTextView;*/

        /*mSearchPage = findViewById(R.id.search_page);
        mSearchPanel = findViewById(R.id.search_panel);
        mSearchFormLayout = findViewById(R.id.search_form_layout);
        mSearchIcon = findViewById(R.id.search_icon);
        mSearchTextLayout = findViewById(R.id.search_text_layout);
        mSearchLabelTextView = findViewById(R.id.search_text_label);
        mSearchEditText = findViewById(R.id.search_edit_text);*/
    }

    private void initLeftPanel(){}

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
            Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_SHORT);
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

    //**********************************************
    //          ACTION BAR
    //**********************************************
    public void hideActionBarHomeButton() {
    }

    public void showActionBarHomeButton(){}

    public void setActionBarBackground(int res) {
        if (mActionBar != null) {
            Drawable drawable = getResources().getDrawable(res);
            mActionBar.setBackgroundDrawable(drawable);
        }
    }

    public void setActionBarTitle(String title) {
        if (mActionBar != null) {
            mActionBar.setTitle(title);
        }
    }

    public void setActionBarHomeIcon(int resIcon) {
        if (mActionBar != null) {
            mActionBar.setHomeButtonEnabled(true);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(resIcon);
        }
    }

    public void setActionBarHomeIcon(String url){
        if (mActionBar == null || url == null || "".equals(url) ) {
            return;
        }

        Background.go(() -> {
            Bitmap bitmap;
            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            try (InputStream in = new BufferedInputStream(new URL(url).openStream(), 8192); BufferedOutputStream out = new BufferedOutputStream(dataStream, 8192)) {
                copy(in, out);
                out.flush();
                final byte[] data = dataStream.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                Handler h = new Handler(Application.context().getMainLooper());
                Bitmap finalBitmap = bitmap;
                h.post(()-> {
                    mActionBar.setHomeButtonEnabled(true);
                    Drawable drawable = new BitmapDrawable(getResources(), finalBitmap);
                    mActionBar.setHomeAsUpIndicator(drawable);
                });
            } catch (IOException e) {
                Log.e("BitmapLoader", "Could not loadBitmap Bitmap from: " + url);
            }
        });
    }

    public void setActionBarActions(final ActionData[] actions) {
        for (ActionData action: actions ) {
            LinearLayout actionContainer = new LinearLayout(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
            params.leftMargin = (int) getResources().getDimension(R.dimen.default_spacing);
            params.rightMargin = (int) getResources().getDimension(R.dimen.default_spacing);

            actionContainer.setLayoutParams(params);
            actionContainer.setOrientation(LinearLayout.HORIZONTAL);
            actionContainer.setBackgroundResource(R.drawable.action_bar_item_background);
            actionContainer.setClickable(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                actionContainer.setFocusable(View.FOCUSABLE);
            }

            ImageView img = new ImageView(this);
            img.setScaleX((float) 0.7);
            img.setScaleY((float) 0.7);
            img.setColorFilter(getResources().getColor(R.color.action_bar_icon_color));
            int dim = (int) getResources().getDimension(R.dimen.action_bar_button_dim);

            if (action.iconBitmap != null) {
                img.setImageBitmap(action.iconBitmap);
            } else {
                img.setImageResource(action.iconResource);
            }
            actionContainer.addView(img, dim, dim);
            actionContainer.setOnClickListener((v)-> {
                //PydioActivity.this.onActionBarActionClicked(action.tag, v);
            });
        }
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
    //          SEARCH
    //**********************************************
    public void requestSearch(String[] completionList) {}

    public void hideSearch() {}

    protected void onSearchRequested(String searchText) {

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

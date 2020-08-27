package com.pydio.android.cells;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Resources;
import com.pydio.android.client.data.State;
import com.pydio.android.client.gui.activities.Accounts;
import com.pydio.android.client.gui.activities.Browser;

public class StartActivity extends AppCompatActivity {

    LinearLayout splashBackground;
    boolean retrievedHeight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.setContentView(R.layout.activity_splash_layout);
        splashBackground = findViewById(R.id.splash_background);
        splashBackground.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(!retrievedHeight){
                retrievedHeight = true;
                int height = splashBackground.getMeasuredHeight();
                int width = splashBackground.getMeasuredWidth();
                Application.setPreference(Application.PREF_PREVIOUS_SCREEN_WIDTH, String.valueOf(width));
                Application.setPreference(Application.PREF_PREVIOUS_SCREEN_HEIGHT, String.valueOf(height));
            }
        });

        int darkMainColor = -1;
        Bitmap b = Resources.splashBackgroundImage();
        if (b != null) {
            ((ImageView) findViewById(com.pydio.android.client.R.id.icon)).setImageBitmap(b);
        }

        int bg_color = Resources.backgroundColor();
        if (bg_color != -1) {
            (findViewById(com.pydio.android.client.R.id.content)).setBackgroundColor(bg_color);
            darkMainColor = Resources.darkenColor(bg_color);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (darkMainColor == -1) {
                window.setStatusBarColor(getResources().getColor(R.color.splashColor));
            } else {
                window.setStatusBarColor(darkMainColor);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Handler h = new Handler(this.getMainLooper());
        h.postDelayed(() -> {
            State state = Application.loadState();
            if (state == null) {
                Intent intent = new Intent();
                if (Application.sessions.size() > 0) {
                    intent.setClass(StartActivity.this, Accounts.class);
                } else {
                    intent.setClass(StartActivity.this, Application.loginClass);
                }
                StartActivity.this.startActivity(intent);
                StartActivity.this.finish();
                return;
            }

            state.setSaver((v, e) -> Application.setPreference(Application.PREF_APP_STATE, v));
            Intent intent = new Intent(StartActivity.this, Browser.class);
            intent.putExtra("state", state.toString());
            StartActivity.this.startActivity(intent);
            StartActivity.this.finish();
        }, 2000);
    }

}

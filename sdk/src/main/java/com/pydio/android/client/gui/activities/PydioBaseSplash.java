package com.pydio.android.client.gui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Resources;
import com.pydio.android.client.data.State;

import java.util.Properties;

public class PydioBaseSplash extends FragmentActivity {

    protected long duration = 1500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash_layout);

        Properties p = Application.internalConfigs();
        if(Boolean.parseBoolean(p.getProperty("resolution", "false"))){
            findViewById(R.id.splash_background).setVisibility(View.GONE);
            (findViewById(R.id.content)).setBackgroundColor(getResources().getColor(R.color.main_color));
            findViewById(R.id.icon).setBackgroundResource(R.drawable.main_icon);
        } else {
            findViewById(R.id.icon).setBackgroundResource(R.drawable.logo);
        }

        int darkMainColor = -1;
        Bitmap b = Resources.splashBackgroundImage();
        if (b != null) {
            ((ImageView) findViewById(R.id.icon)).setImageBitmap(b);
        }
        int bg_color = Resources.backgroundColor();
        if (bg_color != -1) {
            (findViewById(R.id.splash_background)).setBackgroundColor(bg_color);
            (findViewById(R.id.content)).setBackgroundColor(bg_color);
            darkMainColor = Resources.darkenColor(bg_color);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if(darkMainColor == -1) {
                window.setStatusBarColor(getResources().getColor(R.color.main_color_dark));
            } else {
                window.setStatusBarColor(darkMainColor);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Handler handler = new Handler();
        handler.postDelayed(this::start, duration);
    }

    protected void start(){
        State state = Application.loadState();
        if (state == null) {
            Intent intent = new Intent();
            intent.setClass(PydioBaseSplash.this, Application.loginClass);
            PydioBaseSplash.this.startActivity(intent);
            PydioBaseSplash.this.finish();
            return;
        }

        state.setSaver((v, e)-> Application.setPreference(Application.PREF_APP_STATE, v));
        Intent intent = new Intent(PydioBaseSplash.this, Browser.class);
        intent.putExtra("state", state.toString());
        PydioBaseSplash.this.startActivity(intent);
        PydioBaseSplash.this.finish();
    }
}

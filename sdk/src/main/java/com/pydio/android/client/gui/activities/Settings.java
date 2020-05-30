package com.pydio.android.client.gui.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Resources;

public class Settings extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_layout);
        applyThemeColor();
        findViewById(R.id.server_refresh_group).setVisibility(View.GONE);
        findViewById(R.id.backup_group).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSecurityPin();
        refreshSavePassword();
        refreshTransferConnection();
        refreshDownloadImagePreview();
        refreshCachePrune();
        refreshCacheSize();
    }

    private void refreshSecurityPin() {
        final Switch switcher = findViewById(R.id.enable_pin_switch);
        switcher.setEnabled(false);
        switcher.setOnCheckedChangeListener(null);
        switcher.clearFocus();
    }

    private void refreshSavePassword() {
        final Switch switcher = findViewById(R.id.save_password_switch);
        switcher.setChecked(false);
        switcher.setEnabled(false);
    }

    private void refreshTransferConnection() {
        final Switch switcher = findViewById(R.id.enable_transfer_switch);
        final boolean savePassword = "true".equals(Application.getPreference(Application.PREF_NETWORK_3G_TRANSFER));
        switcher.setOnCheckedChangeListener(null);
        switcher.setChecked(savePassword);
        switcher.clearFocus();

        switcher.setOnCheckedChangeListener((compoundButton, checked) -> {
            Application.setPreference(Application.PREF_NETWORK_3G_TRANSFER, String.valueOf(checked).toLowerCase());
            refreshTransferConnection();
        });

        findViewById(R.id.network_transfer_option).setOnClickListener(view -> {
            final boolean savePassword1 = "true".equals(Application.getPreference(Application.PREF_NETWORK_3G_TRANSFER));
            Application.setPreference(Application.PREF_NETWORK_3G_TRANSFER, String.valueOf(!savePassword1).toLowerCase());
            refreshTransferConnection();
        });

    }

    private void refreshDownloadImagePreview() {
        final Switch switcher = findViewById(R.id.thumbnail_load_switch);
        final boolean savePassword = "true".equals(Application.getPreference(Application.PREF_NETWORK_3G_PREVIEW));
        switcher.setOnCheckedChangeListener(null);
        switcher.setChecked(savePassword);
        switcher.setOnCheckedChangeListener((compoundButton, checked) -> {
            Application.setPreference(Application.PREF_NETWORK_3G_PREVIEW, String.valueOf(checked).toLowerCase());
            refreshDownloadImagePreview();
        });
        findViewById(R.id.network_image_cache_option).setOnClickListener(view -> {
            final boolean savePassword1 = "true".equals(Application.getPreference(Application.PREF_NETWORK_3G_PREVIEW));
            Application.setPreference(Application.PREF_NETWORK_3G_PREVIEW, String.valueOf(!savePassword1).toLowerCase());
            refreshDownloadImagePreview();
        });
        switcher.clearFocus();
    }

    private void refreshCachePrune() {
        SeekBar seekbar = findViewById(R.id.cache_pruner_seek_bar);
        seekbar.setEnabled(false);
        seekbar.setProgress(0);
        seekbar.clearFocus();
    }

    private void refreshCacheSize() {
        SeekBar seekbar = findViewById(R.id.cache_size_seek_bar);
        seekbar.setEnabled(false);
        seekbar.setProgress(0);
        seekbar.clearFocus();
    }

    public void applyThemeColor() {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (Application.customTheme() != null) {
                window.setStatusBarColor(Application.customTheme().getMainColor());
            } else {
                int darkBackgroundColor = Resources.darkBackgroundColor();
                if (darkBackgroundColor == -1) {
                    window.setStatusBarColor(getResources().getColor(R.color.status_bar_color));
                } else {
                    window.setStatusBarColor(darkBackgroundColor);
                }
            }
        }

        int color = Resources.backgroundColor();
        int oppositeColor = Resources.oppositeBackgroundColor();
        /*if(color != -1){
            findViewById(R.id.action_bar).setBackgroundColor(color);
            ((TextView)findViewById(R.id.action_bar_title)).setTextColor(oppositeColor);
            ((ImageView)findViewById(R.id.action_leave_icon)).setColorFilter(oppositeColor);

            int dark = Resources.darkMainColor();
            ((TextView)findViewById(R.id.security_title)).setTextColor(dark);
            ((TextView)findViewById(R.id.cache_title)).setTextColor(dark);
            ((TextView)findViewById(R.id.network_title)).setTextColor(dark);
            ((TextView)findViewById(R.id.server_refresh_title)).setTextColor(dark);
        }*/
    }
}

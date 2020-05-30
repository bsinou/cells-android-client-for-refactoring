package com.pydio.android.client.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.pydio.android.client.data.Application;

import java.util.ArrayList;

public class Connectivity extends BroadcastReceiver {

    public static final int NONE = 0;
    public static final int WIFI = 1;
    public static final int MOBILE = 2;
    private static ArrayList<OnConnectionStateChangeListener> listeners;
    boolean wifi, mobile, internet;

    public static void addConnectionStateListener(OnConnectionStateChangeListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<OnConnectionStateChangeListener>();
        }
        listeners.add(listener);
    }

    public static void removeConnectionStateListener(OnConnectionStateChangeListener listener) {
        if (listeners == null) return;
        listeners.remove(listener);
    }

    public static void publishConnectionNewState(int state) {
        if (listeners == null) return;
        for (int i = 0; i < listeners.size(); i++) {
            OnConnectionStateChangeListener l = listeners.get(i);
            l.onStateChange(state);
        }
    }

    public static boolean isWifiConnected() {
        ConnectivityManager conn = (ConnectivityManager) Application.context().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static boolean isMobileConnected() {
        ConnectivityManager conn = (ConnectivityManager) Application.context().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isConnectedToTheInternet() {
        return isWifiConnected() || isMobileConnected();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        final int state[] = new int[1];

        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                state[0] = WIFI;
                internet = wifi = !(mobile = false);

            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                state[0] = MOBILE;
                internet = mobile = !(wifi = false);

            } else {
                state[0] = NONE;
                internet = mobile = wifi = false;
            }
        } else {
            state[0] = NONE;
            internet = mobile = wifi = false;
        }

        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            @Override
            public void run() {
                publishConnectionNewState(state[0]);
                pr.finish();
            }
        }).start();

    }

    public interface OnConnectionStateChangeListener {
        void onStateChange(final int con);
    }
}

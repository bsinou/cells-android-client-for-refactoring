package com.pydio.android.client.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Connectivity {
    private boolean connected;
    private boolean cellularDownloadAllowed;
    private boolean cellularImagePreviewDownloadAllowed;
    private boolean cellular;

    public boolean isCellular(){
        return cellular;
    }

    public boolean icConnected(){
        return connected;
    }

    public boolean isCellularDownloadAllowed(){
        return cellularDownloadAllowed;
    }

    public boolean isCellularImagePreviewDownloadAllowed(){
        return cellularImagePreviewDownloadAllowed;
    }

    public static Connectivity get(Context context){
        Connectivity con = new Connectivity();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null){
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null){
                con.connected = true;
                int type = activeNetworkInfo.getType();
                con.cellular = type == ConnectivityManager.TYPE_MOBILE;
            } else {
                con.connected = false;
            }
        } else {
            con.connected = false;
        }

        con.cellularDownloadAllowed = "true".equals(Application.getPreference(Application.PREF_NETWORK_3G_TRANSFER));
        con.cellularImagePreviewDownloadAllowed = "true".equals(Application.getPreference(Application.PREF_NETWORK_3G_PREVIEW));
        return con;
    }
}

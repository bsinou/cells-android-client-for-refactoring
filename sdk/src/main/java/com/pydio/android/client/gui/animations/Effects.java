package com.pydio.android.client.gui.animations;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;

import com.pydio.android.client.R;

public class Effects {

    public static void clicked(Context c, View v){
        if(v == null) return;
        int delay = 100;
        AnimationDrawable a = new AnimationDrawable();
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white)), 700);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white0)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white1)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white2)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white2)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white1)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white0)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white)), delay);
        a.setOneShot(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            v.setBackground(a);
        } else {
            v.setBackgroundDrawable(a);
        }
        a.start();
    }

    public static void pressed(Context c, View v){
        if(v == null) return;
        int delay = 100;

        AnimationDrawable a = new AnimationDrawable();
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white)), 500);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white0)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white1)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white2)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white2)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white2)), delay);
        a.setOneShot(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            v.setBackground(a);
        } else {
            v.setBackgroundDrawable(a);
        }
        a.start();
    }

    public static void released(Context c, View v){
        if(v == null) return;
        int delay = 100;

        AnimationDrawable a = new AnimationDrawable();
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white2)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white2)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white1)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white1)), delay);
        a.addFrame(new ColorDrawable(c.getResources().getColor(R.color.white)), delay);
        a.setOneShot(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            v.setBackground(a);
        } else {
            v.setBackgroundDrawable(a);
        }
        a.start();
    }
}

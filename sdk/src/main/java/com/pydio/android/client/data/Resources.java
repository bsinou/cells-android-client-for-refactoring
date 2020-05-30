package com.pydio.android.client.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import com.pydio.android.client.R;

import java.util.HashMap;

public class Resources {

    private static final float DARKEN_FACTOR = 0.8f;

    private static Bitmap splashImage = null;
    private static int splashBgColor = -1;

    private static int mainColor = -1;
    private static int oppositeMainColor = -1;
    private static int oppositeBackgroundColor = -1;
    private static int darkenedMainColor = -1;
    private static int darkenedBackgroundColor = -1;

    private static HashMap<Integer, Integer> iconColorMap = null;

    public static Bitmap splashBackgroundImage(){
        if(splashImage == null) {
            String path = Application.getPreference(Application.PREF_SPLASH_IMAGE_PATH);
            if (path != null && !"".equals(path))
                try {
                    splashImage = BitmapFactory.decodeFile(path);
                } catch (OutOfMemoryError e) {
                    return null;
                }
        }
        return splashImage;
    }

    public static int backgroundColor(){
        if(splashBgColor == -1) {
            String client  = Application.getPreference("clientID");
            if(client == null || client.length() == 0) return -1;
            String c = Application.getPreference(client + "_" + Application.PREF_BACKGROUND_COLOR);
            if (c != null && !"".equals(c)) {
                if(!c.startsWith("#")){
                    c  = "#" + c;
                }
                try {
                    splashBgColor = Color.parseColor(c);
                }catch (Exception e) {
                    //Log.e("Color", e.getMessage());
                    return -1;
                }
            }
        }
        return splashBgColor;
    }

    public static int darkBackgroundColor(){
        if(darkenedBackgroundColor == -1) {
            darkenedBackgroundColor = darkenColor(backgroundColor());
        }
        return darkenedBackgroundColor;
    }

    public static int mainColor(){
        if(mainColor == -1) {
            String client  = Application.getPreference("clientID");
            if(client == null || client.length() == 0) return -1;

            String c = Application.getPreference(client + "_" + Application.PREF_MAIN_COLOR);
            if (c != null && !"".equals(c)) {
                try {
                    return mainColor = Color.parseColor(c);
                }catch (Exception e) {
                    //Log.e("Color", e.getMessage());
                    if(!c.startsWith("#")){
                        c  = "#" + c;
                    }
                }
                try {
                    return mainColor = Color.parseColor(c);
                }catch (Exception e) {
                    //Log.e("Color", e.getMessage());
                    return -1;
                }
            }
        }
        return mainColor;
    }

    public static int parseColor(String color) {
        try {
            return mainColor = Color.parseColor(color);
        }catch (Exception e) {
            if(!color.startsWith("#")){
                color  = "#" + color;
            }
        }
        try {
            return mainColor = Color.parseColor(color);
        }catch (Exception e) {
            //Log.e("Color", e.getMessage());
            return -1;
        }
    }

    public static int darkMainColor(){
        if(darkenedMainColor == -1) {
            darkenedMainColor = darkenColor(mainColor());
        }
        return darkenedMainColor;
    }

    public static int oppositeMainColor(){
        if(oppositeMainColor == -1){
            oppositeMainColor = oppositeColor(mainColor());
        }
        return oppositeMainColor;
    }

    public static int oppositeBackgroundColor() {
        if(oppositeBackgroundColor == -1){
            oppositeBackgroundColor = oppositeColor(backgroundColor());
        }
        return oppositeBackgroundColor;
    }

    public static int darkenColor(int color){
        //thanks to http://stackoverflow.com/users/535871/ted-hopp from http://stackoverflow.com/questions/4928772/android-color-darker
        if(color == -1) return -1;
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= DARKEN_FACTOR; // value component
        return Color.HSVToColor(hsv);
    }

    public static Drawable drawable(Context ctx, int res, int color){
        int colorValue = ctx.getResources().getColor(color);
        Drawable d = ctx.getResources().getDrawable(res);
        d.mutate().setColorFilter(colorValue, PorterDuff.Mode.SRC_IN);
        return d;
    }

    public static int oppositeColor(int color) {
        if(Color.BLACK == color) return Color.WHITE;
        if(Color.WHITE == color) return Color.BLACK;

        if(mainColor == -1) return -1;
        // drawable existing colors
        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);

        // find compliments
        red = (~red) & 0xff;
        blue = (~blue) & 0xff;
        green = (~green) & 0xff;

        if ((red*0.299 + green*0.587 + blue*0.114) > 186){
            return Color.BLACK;
        }  else {
            return Color.WHITE;
        }
        //return Color.argb(alpha, red, green, blue);
    }

    public static void clear(){
        splashImage = null;
        splashBgColor = -1;
        mainColor = -1;
        oppositeMainColor = -1;
        darkenedMainColor = -1;
    }

    @SuppressLint("UseSparseArrays")
    public static int iconColor(int iconRes){
        if(iconColorMap == null){
            iconColorMap = new HashMap<>();
            iconColorMap.put(R.drawable.image, R.color.material_red);
            iconColorMap.put(R.drawable.docx, R.color.material_blue);
            iconColorMap.put(R.drawable.doc, R.color.material_blue);
            iconColorMap.put(R.drawable.word, R.color.material_blue);
            iconColorMap.put(R.drawable.apk, R.color.android_green);
            iconColorMap.put(R.drawable.pdf, R.color.material_red);
            iconColorMap.put(R.drawable.mp3, R.color.material_orange);
            iconColorMap.put(R.drawable.audio, R.color.material_orange);
            iconColorMap.put(R.drawable.wma, R.color.material_orange);
            iconColorMap.put(R.drawable.video, R.color.material_deep_orange);
            iconColorMap.put(R.drawable.mp4, R.color.material_deep_orange);
            iconColorMap.put(R.drawable.flv, R.color.material_deep_orange);
            iconColorMap.put(R.drawable.rocket, R.color.orange1);
            iconColorMap.put(R.drawable.ppt, R.color.material_indigo);
            iconColorMap.put(R.drawable.pptx, R.color.material_indigo);
            iconColorMap.put(R.drawable.xls, R.color.material_green);
            iconColorMap.put(R.drawable.xlt, R.color.material_green);
        }

        if(iconColorMap.containsKey(iconRes)) {
            return iconColorMap.get(iconRes);
        }
        return R.color.icon_color_filter;
    }
}

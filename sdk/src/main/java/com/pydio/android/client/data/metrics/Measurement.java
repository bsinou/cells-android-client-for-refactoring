package com.pydio.android.client.data.metrics;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;

public class Measurement {

    public static int browserFragmentWidth;
    public static int contentHeight;
    private static int statusBarHeight = -1;

    public static int getDeviceNavigationBarheight(Context c){
        android.content.res.Resources resources = c.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    public static int statusBarHeight(Activity c){
        if(statusBarHeight == -1) {
            Rect rectangle= new Rect();
            Window window= c.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
            statusBarHeight = rectangle.top;
        }
        return statusBarHeight;
    }
    public static int getScreen_height(Context c){
        Point p = getAppUsableScreenSize(c);
        return p.y;
    }
    public static int getScreen_width(Context c){
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        if(wm != null) {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            return size.x;
        }
        return -1;
    }

    public static Point getNavigationBarSize(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);

        // navigation bar on the side
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }

        // navigation bar is not present
        return new Point();
    }

    public static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception ignored) {}
        }

        return size;
    }


    public static boolean smallScreen(Context c){
        return Measurement.getScreen_width(c) <= Application.context().getResources().getDimension(R.dimen.small_screen_width);
    }
    public static boolean largeScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static int grid_behind_item_width;
    public static int list_behind_item_width;
    public static int grid_behind_item_height;
    public static int grid_cell_number_of_options_item;
    public static int grid_behind_max_swipe_distance;
}

package com.pydio.android.client.gui.view.group;

import android.content.Context;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Display;

public class DisplayConfig {

    private static DisplayConfig defaultConfig;

    private DisplayConfig(Context c){
        metrics = new Metrics(c);
    }

    private Metrics metrics;

    public Metrics getMetrics() {
        return metrics;
    }

    public static DisplayConfig getDefault(Context c){
        if(defaultConfig == null){
            defaultConfig = new DisplayConfig(c);
            int viewType = Display.list;
            defaultConfig.metrics.setHorizontalSpacing(viewType, R.dimen.list_v_gutter);
            defaultConfig.metrics.setVerticalSpacing(viewType, R.dimen.list_h_gutter);
            defaultConfig.metrics.setItemDefaultWidth(viewType, R.dimen.list_cell_width);
            defaultConfig.metrics.setItemDefaultHeight(viewType, R.dimen.list_cell_height);

            viewType = Display.grid;
            defaultConfig.metrics.setHorizontalSpacing(viewType, R.dimen.grid_v_gutter);
            defaultConfig.metrics.setVerticalSpacing(viewType, R.dimen.grid_h_gutter);
            defaultConfig.metrics.setItemDefaultWidth(viewType, R.dimen.grid_cell_width);
            defaultConfig.metrics.setItemDefaultHeight(viewType, R.dimen.grid_cell_height);
        }
        return defaultConfig;
    }
}

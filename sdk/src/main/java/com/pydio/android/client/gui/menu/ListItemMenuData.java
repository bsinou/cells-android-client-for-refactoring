package com.pydio.android.client.gui.menu;

import android.view.View;

import com.pydio.android.client.gui.menu.models.ActionData;

import java.util.List;

public class ListItemMenuData {
    public String label;
    public int resIcon;
    public boolean hasOption;
    public List<ActionData> actions;
    public View.OnClickListener infoClickListener;
}

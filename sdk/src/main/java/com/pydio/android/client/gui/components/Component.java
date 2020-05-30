package com.pydio.android.client.gui.components;

import android.view.View;

public abstract class Component {

    public abstract View getView();

    protected void initView(){}

    public void show() {
        getView().setVisibility(View.VISIBLE);
    }

    public void hide(){
        getView().setVisibility(View.GONE);
    }
}

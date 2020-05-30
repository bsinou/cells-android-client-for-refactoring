package com.pydio.android.client.gui.view.group;

public class LayoutParameters extends android.view.ViewGroup.LayoutParams {
    public int leftSpace;
    public int topSpace;
    public int rightSpace;
    public int bottomSpace;

    public LayoutParameters(int width, int height) {
        super(width, height);
    }

    public LayoutParameters(android.view.ViewGroup.LayoutParams source) {
        super(source);
    }
}


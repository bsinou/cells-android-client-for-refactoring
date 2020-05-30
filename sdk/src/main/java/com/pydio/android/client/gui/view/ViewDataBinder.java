package com.pydio.android.client.gui.view;

import android.view.View;

public interface ViewDataBinder {
    View createView(int type);
    void bindData(View view, int position);
}

package com.pydio.android.client.gui.view.group;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

public abstract class ViewGroupAdapter {

    private DataSetObserver mDataObserver;

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataObserver = observer;
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (mDataObserver == observer){
            mDataObserver = null;
        }
    }

    public boolean hasStableIds() {
        return false;
    }

    public void notifyDataSetChanged(){
        if(mDataObserver != null){
            mDataObserver.onChanged();
        }
    }

    public abstract int getCount();

    public abstract Object getItem(int position);

    public abstract long getItemId(int position);

    public abstract View getView(int position, View v, ViewGroup parent);

    public abstract int getItemViewType(int position);

    public abstract int getViewTypeCount();

    public abstract LayoutParameters getLayoutParams(int type);

    public abstract boolean isEmpty();

}

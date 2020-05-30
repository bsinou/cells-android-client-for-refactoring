package com.pydio.android.client.gui.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pydio.android.client.data.listing.NodeDataSet;
import com.pydio.android.client.gui.view.ViewDataBinder;
import com.pydio.sdk.core.model.Node;

public class NodeListAdapter extends RecyclerView.Adapter<NodeListAdapter.ViewHolder> {

    public interface ImageThumbLoader {
        void loadBitmap(ImageView image, Node node, int dim);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View view;
        ViewHolder(View v) {
            super(v);
            this.view = v;
        }
    }

    private NodeDataSet dataSet;
    private ViewDataBinder viewDataBinder;

    public NodeListAdapter(ViewDataBinder binder) {
        this.viewDataBinder = binder;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        View view = this.viewDataBinder.createView(type);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        this.viewDataBinder.bindData(viewHolder.view, i);
    }
    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void update(NodeDataSet dataSet) {
        this.dataSet = dataSet;
    }
}

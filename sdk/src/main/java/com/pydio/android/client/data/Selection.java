package com.pydio.android.client.data;

import android.content.Context;

import com.pydio.android.client.R;
import com.pydio.sdk.core.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Selection implements Node {
    private boolean allSelected;
    private Node in;
    private List<Node> list;
    private Context context;
    private boolean forMove;

    public Selection(Context context, Node in) {
        this.context = context;
        this.in = in;
        this.list = new ArrayList<>();
    }

    public Node in() {
        return this.in;
    }

    public int index(Node node) {
        int index = -1;
        for (Node n: this.list) {
            index++;
            if (node == n) {
                return index;
            }
        }
        return -1;
    }

    public void addOrRemoveNode(Node node) {
        final int index = index(node);
        if (index == -1) {
            list.add(node);
        } else {
            list.remove(index);
        }
    }

    public void clear() {
        this.list.clear();
    }

    public void update(List<Node> list) {
        this.clear();
        this.list = list;
    }

    public void setAllSelected(boolean allSelected) {
        this.allSelected = allSelected;
    }

    public boolean isAllSelected() {
        return this.allSelected;
    }

    public List<Node> nodes(){
        return new ArrayList<>(list);
    }

    public void setForMove(boolean forMove) {
        this.forMove = forMove;
    }

    public boolean isForMove() {
        return this.forMove;
    }

    @Override
    public int type() {
        return Node.TYPE_SELECTION;
    }

    @Override
    public String id() {
        return "selection";
    }

    @Override
    public String label() {
        int size = list.size();
        if (size == 0) {
            return this.context.getString(R.string.no_item_selected);
        } else {
            return String.format(this.context.getString(R.string.selected_count), list.size());
        }
    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public void setProperty(String key, String value) {

    }

    @Override
    public void deleteProperty(String key) {

    }

    @Override
    public void setProperties(Properties p) {

    }

    @Override
    public String getEncoded() {
        return null;
    }

    @Override
    public int compare(Node node) {
        return 0;
    }

    @Override
    public String getEncodedHash() {
        return null;
    }
}

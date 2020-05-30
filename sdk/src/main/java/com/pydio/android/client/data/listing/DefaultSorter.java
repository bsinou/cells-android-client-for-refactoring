package com.pydio.android.client.data.listing;

import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Node;

public class DefaultSorter implements Sorter<Node> {
    @Override
    public boolean isBefore(Node n1, Node n2) {

        if(!(n1 instanceof FileNode) || !(n2 instanceof FileNode)){
            return n1.label().compareToIgnoreCase(n2.label()) <= 0;
        }


        if(n1.path().toLowerCase().equals("/recycle_bin")) return false;
        if(n2.path().toLowerCase().equals("/recycle_bin")) return true;
        if (((FileNode)n1).isFile() && ((FileNode)n2).isFile()) {
            return false;
        }

        if (!((FileNode)n1).isFile() && ((FileNode)n2).isFile()) {
            return true;
        }
        return n1.label().compareToIgnoreCase(n2.label()) <= 0;
    }
}

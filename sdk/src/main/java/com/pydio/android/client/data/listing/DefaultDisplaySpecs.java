package com.pydio.android.client.data.listing;

import com.pydio.sdk.core.model.Node;

import java.io.Serializable;

public class DefaultDisplaySpecs implements DisplaySpecs<Node>, Serializable{
    int total_count = 0;

    @Override
    public void removeSection(int index){}
    @Override
    public void addSection(Section s){}
    @Override
    public int sectionsCount(){
        return 0;
    }
    @Override
    public Section sectionAt(int index) {
        return null;
    }
    @Override
    public int sectionIndex(Section s) {
        return -1;
    }
    @Override
    public Node update(Node node) {
        total_count++;
        return null;
    }
    @Override
    public void clear() {}

    public boolean isTitleIndex(int i){
        return false;
    }
    @Override
    public int totalCount() {
        return total_count;
    }
}

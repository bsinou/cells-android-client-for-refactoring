package com.pydio.android.client.data.listing;


import com.pydio.sdk.core.model.Node;

public interface DisplaySpecs<T> {

    int MODE_LIST = 1;
    int MODE_GRID = 2;

    void removeSection(int index);
    void addSection(Section s);
    int sectionsCount();
    Section sectionAt(int index);
    int sectionIndex(Section s);
    Node update(T t);
    void clear();
    int totalCount();

    abstract class Section<T>{
        public int display_mode;
        public int res_title;
        public int items_count;

        public Section(){
            res_title = 0;
            items_count = 0;
            display_mode = MODE_LIST;
        }

        public Section(int title){
            res_title = title;
            items_count = 0;
            display_mode = MODE_GRID;
        }

        public abstract boolean match(T t);
    }
}

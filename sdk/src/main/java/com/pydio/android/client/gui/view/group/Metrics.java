package com.pydio.android.client.gui.view.group;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseArray;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Display;

public class Metrics {

    private int itemMinWidth;
    private int verticalSpacing;
    private int horizontalSpacing;

    private SparseArray<Integer> defaultWidths = new SparseArray<>();
    private SparseArray<Integer> defaultHeights = new SparseArray<>();

    private SparseArray<Integer> colCounts = new SparseArray<>();

    private SparseArray<Integer> widths = new SparseArray<>();
    private SparseArray<Integer> heights = new SparseArray<>();

    private SparseArray<Integer> verticalSpacings = new SparseArray<>();
    private SparseArray<Integer> horizontalSpacings = new SparseArray<>();

    private Context context;
    private Resources res;

    public Metrics(Context c){
        this.context = c;
        this.res = c.getResources();
    }

    private int calculateItemWidth(int viewType, int rowWidth) {
        int minWidth;
        Integer mw = defaultWidths.get(viewType);
        if (mw != null) {
            minWidth = mw;

            float widthLeft = rowWidth - verticalSpacing;
            int itemSpace = minWidth + verticalSpacing;

            int columnCount = (int) (widthLeft / itemSpace);
            float rest = widthLeft - (columnCount * itemSpace);

            if (rest > 0) {
                itemSpace += rest / columnCount;
            }

            int itemWidth = itemSpace - verticalSpacing;
            widths.put(viewType, itemWidth);
            colCounts.put(viewType, columnCount);
            return itemWidth;

        } else {
            minWidth = -1;
            colCounts.put(viewType, 1);
            return minWidth;
        }
    }

    public int columnCount(int type){
        Integer count = colCounts.get(type);

        if(count != null){
            return count;
        }
        return 1;
    }

    public void calculateItemsWidth(int groupWidth){
        for (int i = 0; i < defaultWidths.size(); i++){
            int key = defaultWidths.keyAt(i);
            calculateItemWidth(key, groupWidth);
        }
    }

    public int[] getItemDims(int viewType) {
        Integer w = widths.get(viewType);
        Integer h = heights.get(viewType);

        if (w == null){
            w = defaultWidths.get(viewType);
        }

        if (h == null){
            h = defaultHeights.get(viewType);
        }

        return new int[]{w, h};
    }

    public int getHorizontalSpacing(int viewType){
        Integer s = horizontalSpacings.get(viewType);
        if (s == null){
            return 0;
        }
        return s;
    }

    public int getVerticalSpacing(int viewType){
        Integer s = verticalSpacings.get(viewType);
        if (s == null){
            return 0;
        }
        return s;
    }

    public void setHorizontalSpacing(int viewType, int spacing){
        horizontalSpacings.put(viewType, (int) res.getDimension(spacing));
    }

    public void setVerticalSpacing(int viewType, int spacing){
        verticalSpacings.put(viewType, (int) res.getDimension(spacing));
    }

    public void setItemDefaultHeight(int viewType, int h){
        int height = (int) res.getDimension(h);
        if (viewType == Display.grid) {
            height += res.getDimension(R.dimen.grid_cell_bottom_height);
        }
        defaultHeights.put(viewType, height);
    }

    public void setItemDefaultWidth(int viewType, int w) {
        if(w == ViewGroup.LayoutParams.MATCH_PARENT){
            defaultWidths.put(viewType, w);
        } else {
            defaultWidths.put(viewType, (int) res.getDimension(w));
        }
    }
}


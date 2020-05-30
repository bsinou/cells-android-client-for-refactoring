package com.pydio.android.client.gui.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Display;
import com.pydio.android.client.data.Resources;
import com.pydio.android.client.data.nodes.NodeUtils;
import com.pydio.android.client.gui.adapters.NodeListAdapter;
import com.pydio.android.client.gui.view.group.Metrics;

public class ViewWrapper {

    private View rootView;
    private ViewData data;

    private ImageView icon, selectedIcon, unselectedIcon;
    private TextView mainText;
    private TextView secondaryText;
    private LinearLayout syncFlag, shareFlag, starredFlag, treeDots, secondaryActionLayout, selectionLayout;
    private Context context;
    private Metrics metrics;

    public ViewWrapper(View rootView, int viewType, Metrics metrics) {
        if (viewType == Display.grid) {
            if (rootView == null) {
                int type = R.layout.view_grid_cell_layout;
                this.rootView = Application.inflater(null).inflate(type, null);
            } else {
                this.rootView = rootView;
            }

            this.metrics = metrics;
            context = this.rootView.getContext();

            initGridView();
        } else {
            if (rootView == null) {
                int type = R.layout.view_list_cell_layout;
                this.rootView = Application.inflater(null).inflate(type, null);
            } else {
                this.rootView = rootView;
            }

            this.metrics = metrics;
            context = this.rootView.getContext();
            initListView();
        }
    }

    private void initListView() {
        icon = rootView.findViewById(R.id.icon);
        mainText = rootView.findViewById(R.id.main_text);
        secondaryText = rootView.findViewById(R.id.secondary_text);
        syncFlag = rootView.findViewById(R.id.synced_flag);
        shareFlag = rootView.findViewById(R.id.shared_flag);
        starredFlag = rootView.findViewById(R.id.starred_flag);
        secondaryActionLayout = rootView.findViewById(R.id.secondary_action_layout);
        selectedIcon = rootView.findViewById(R.id.selected_icon);
        unselectedIcon = rootView.findViewById(R.id.unselected_icon);
        treeDots = rootView.findViewById(R.id.action_option_layout);
        selectionLayout = rootView.findViewById(R.id.selection_state_layout);

        int[] dims = this.metrics.getItemDims(Display.list);
        this.rootView.setLayoutParams(new ViewGroup.LayoutParams(dims[0], dims[1]));
    }

    private void initGridView(){
        icon = rootView.findViewById(R.id.icon);
        mainText = rootView.findViewById(R.id.main_text);
        syncFlag = rootView.findViewById(R.id.synced_flag);
        shareFlag = rootView.findViewById(R.id.shared_flag);
        starredFlag = rootView.findViewById(R.id.starred_flag);
        secondaryActionLayout = rootView.findViewById(R.id.secondary_action_layout);
        selectedIcon = rootView.findViewById(R.id.selected_icon);
        unselectedIcon = rootView.findViewById(R.id.unselected_icon);
        treeDots = rootView.findViewById(R.id.action_option_layout);
        selectionLayout = rootView.findViewById(R.id.selection_state_layout);

        int[] dims = this.metrics.getItemDims(Display.grid);
        FrameLayout fl = rootView.findViewById(R.id.icon_layout);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) fl.getLayoutParams();
        lp.height = dims[0];
        lp.width = dims[0];
        lp.setMargins(5, 5, 5, 5);
        fl.setBackgroundColor(Color.parseColor("#edf0f2"));
        fl.requestLayout();

        com.pydio.android.client.gui.view.group.ViewGroup.LayoutParams viewLp = new com.pydio.android.client.gui.view.group.ViewGroup.LayoutParams(dims[0], dims[0] + (int) rootView.getResources().getDimension(R.dimen.grid_cell_bottom_height));
        this.rootView.setLayoutParams(viewLp);
    }

    private ViewWrapper() {}

    public static ViewWrapper wrap(View view) {
        ViewWrapper wrapper = new ViewWrapper();
        wrapper.rootView = view;
        wrapper.icon = view.findViewById(R.id.icon);
        wrapper.mainText = view.findViewById(R.id.main_text);
        wrapper.secondaryText = view.findViewById(R.id.secondary_text);
        wrapper.syncFlag = view.findViewById(R.id.synced_flag);
        wrapper.shareFlag = view.findViewById(R.id.shared_flag);
        wrapper.starredFlag = view.findViewById(R.id.starred_flag);
        wrapper.secondaryActionLayout = view.findViewById(R.id.secondary_action_layout);
        wrapper.selectedIcon = view.findViewById(R.id.selected_icon);
        wrapper.unselectedIcon = view.findViewById(R.id.unselected_icon);
        wrapper.treeDots = view.findViewById(R.id.action_option_layout);
        wrapper.selectionLayout = view.findViewById(R.id.selection_state_layout);
        return wrapper;
    }

    public void setData(ViewData data) {
        this.data = data;
    }

    public View getView() {
        return rootView;
    }

    public void refresh(NodeListAdapter.ImageThumbLoader thumbLoader) {
        refreshFirstText();
        refreshSecondText();
        refreshIcon(thumbLoader);
        refreshSynced();
        refreshShared();
        refreshStarred();
        refreshSelection();
        refreshedSelected();

        secondaryActionLayout.setClickable(true);
        secondaryActionLayout.setFocusable(true);
        if (data.optionClickListener != null) {
            treeDots.setOnClickListener(data.optionClickListener);
            selectionLayout.setOnClickListener(data.optionClickListener);
            secondaryActionLayout.setOnClickListener(data.optionClickListener);
        }
    }

    public void refreshFirstText() {
        mainText.setText(data.mainText);
    }

    public void refreshSecondText() {
        if (secondaryText != null) {
            secondaryText.setText(data.secondaryText);
        }
    }

    public void refreshStarred() {
        starredFlag.setVisibility(data.starredFlagVisibility);
    }

    public void refreshSynced() {
        syncFlag.setVisibility(data.syncFlagVisibility);
    }

    public void refreshShared() {
        shareFlag.setVisibility(data.shareFlagVisibility);
    }

    public void refreshedSelected() {
        selectionLayout.setVisibility(data.selectionLayoutVisibility);
        treeDots.setVisibility(data.treeDotsVisibility);
        unselectedIcon.setVisibility(data.unselectedVisibility);
        selectedIcon.setVisibility(data.selectedVisibility);
    }

    public void refreshIcon(NodeListAdapter.ImageThumbLoader thumbLoader) {
        if (data.icon != null) {
            icon.setImageBitmap(data.icon);
            int color = Resources.iconColor(data.iconColorFilter);
            icon.setColorFilter(color);
        } else {
            icon.setImageResource(data.iconRes);
            icon.setColorFilter(this.context.getResources().getColor(data.iconColorFilter));
        }
        icon.setScaleX(data.iconScaleX);
        icon.setScaleY(data.iconScaleY);

        NodeViewTag tag = (NodeViewTag) this.rootView.getTag();
        if (tag != null && NodeUtils.isImage(tag.node)) {
            thumbLoader.loadBitmap(icon, tag.node, 200);
        }
    }

    public void refreshSelection() {
        selectionLayout.setVisibility(data.selectionLayoutVisibility);
        secondaryActionLayout.setVisibility(data.optionsLayoutVisibility);
    }
}

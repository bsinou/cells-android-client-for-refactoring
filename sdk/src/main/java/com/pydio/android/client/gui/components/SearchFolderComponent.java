package com.pydio.android.client.gui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Display;
import com.pydio.android.client.data.PydioAgent;
import com.pydio.android.client.data.listing.ContentPageState;
import com.pydio.android.client.data.listing.Filter;
import com.pydio.android.client.data.listing.NodeDataSet;
import com.pydio.android.client.data.nodes.NodeUtils;
import com.pydio.android.client.data.nodes.OfflineInfo;
import com.pydio.android.client.data.nodes.SelectionInfo;
import com.pydio.android.client.gui.adapters.NodeListAdapter;
import com.pydio.android.client.gui.view.NodeViewTag;
import com.pydio.android.client.gui.view.ViewDataBinder;
import com.pydio.android.client.gui.view.ViewWrapper;
import com.pydio.android.client.gui.view.group.Metrics;
import com.pydio.android.client.gui.view.ViewData;
import com.pydio.sdk.core.model.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFolderComponent extends BrowserPage implements NodeListAdapter.ImageThumbLoader, ViewDataBinder {

    // Views
    private FrameLayout rootView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;

    // Components
    private EmptyContentComponent emptyContentComponent;

    // Data
    private int displayMode;
    private ContentPageState state;
    private String label;
    private NodeDataSet dataSet;
    private NodeListAdapter adapter;
    private Map<ImageView, String> thumbsRequests;
    private Map<String, Bitmap> thumbsCache;
    private String searchedText;
    private long textReceivedTime;

    // Clients
    private PydioAgent agent;

    public SearchFolderComponent(ContentPageState state) {
        this.state = state;
        this.searchedText = "";
        LayoutInflater inflater = LayoutInflater.from(state.activityContext);
        this.rootView = (FrameLayout) inflater.inflate(R.layout.view_swipe_refresh_grid_layout, null, false);
        this.swipeRefreshLayout = rootView.findViewById(R.id.swipe_layout);

        this.swipeRefreshLayout.setOnRefreshListener(this::refresh);
        this.recyclerView = rootView.findViewById(R.id.gridView);

        this.displayMode = this.state.displayInfo.mode();

        this.agent = new PydioAgent(state.session);
        this.label = state.node.label();

        View view = this.rootView.findViewById(R.id.empty_list_layout);
        this.emptyContentComponent = new EmptyContentComponent(view);
        this.emptyContentComponent.setButtonClickListener(this::onEmptyContentActionTriggered);

        this.thumbsRequests = new HashMap<>();
        this.thumbsCache = new HashMap<>();

        this.dataSet = new NodeDataSet();
        this.adapter = new NodeListAdapter(this);
        this.adapter.update(this.dataSet);
        this.recyclerView.setAdapter(adapter);
    }

    @Override
    public int mode() {
        return ModeSearch;
    }

    @Override
    public List<Node> get(Filter filter) {
        return dataSet.filter(filter);
    }

    @Override
    public Node itemAt(int index) {
        return this.dataSet.get(index);
    }

    @Override
    public void refresh() {
        if (this.searchedText.length() == 0) {
            onDataLoaded(new ArrayList<>());
            if (this.dataSet.size() == 0) {
                return;
            }
            return;
        }

        startRefreshing();
        this.agent.search(state.node, this.searchedText, (nodes, error) -> {
            finishRefreshing();
            if (error != null) {
                showMessage(state.activityContext.getString(R.string.failed_to_get_folder_content), label);
                return;
            }
            onDataLoaded(nodes);
        });
    }

    @Override
    public void displayModeChanged() {

    }

    @Override
    public int count() {
        return dataSet.size();
    }

    @Override
    public Node node() {
        return state.node;
    }

    @Override
    public View getView() {
        return rootView;
    }

    @Override
    public void loadBitmap(ImageView image, Node node, int dim) {

    }

    @Override
    public View createView(int type) {
        int display = this.state.displayInfo.mode();
        return new ViewWrapper(null, display, this.state.metrics).getView();
    }

    @Override
    public void bindData(View view, int position) {
        int display = this.state.displayInfo.mode();
        ViewWrapper wrapper = new ViewWrapper(view, display, this.state.metrics);

        SelectionInfo info = this.state.guiContext.getSelectionInfo();
        OfflineInfo offline = this.state.guiContext.getOfflineInfo();
        Context context = this.state.activityContext;

        final Node node = dataSet.get(position);
        ViewData data = ViewData.parse(context, node, offline, info);
        data.setOptionClickListener((v) -> this.itemOptionClickedListener.onClick(node));

        if (display == Display.list) {
            data.setIconScale((float) 0.6, (float) 0.6);
        } else {
            data.setIconScale((float) 0.25, (float) 0.25);
        }
        wrapper.setData(data);
        NodeViewTag tag = new NodeViewTag();
        tag.node = node;
        tag.data = data;
        view.setTag(tag);

        View itemView = wrapper.getView();
        itemView.setOnClickListener((v) -> this.clickListener.onClick(node));
        itemView.setOnLongClickListener(v -> {
            if (this.longClickListener != null) {
                this.longClickListener.onLongClick(node);
            }
            return true;
        });
        ImageView icon = itemView.findViewById(R.id.icon);
        this.loadBitmap(icon, null, 200);
        wrapper.refresh(this);
    }

    @Override
    public void searchRequest(String pattern) {
        if (this.searchedText.equals(pattern)) {
            return;
        }

        this.searchedText = pattern;
        this.textReceivedTime = System.currentTimeMillis();

        this.rootView.postDelayed(() -> {
            long duration = System.currentTimeMillis() - this.textReceivedTime;
            if (duration >= 500) {
                refresh();
            }
        }, 500);
    }

    @Override
    public void loadContent() {
        // Hook to trigger action bar refresh
        this.state.guiContext.refreshActionBar();
        refresh();
    }

    private void onDataLoaded(List<Node> nodes) {
        dataSet = new NodeDataSet();
        dataSet.setSorter(this.state.sorter);
        if (state.mode == ModePaste) {
            final SelectionInfo info = this.state.guiContext.getSelectionInfo();
            dataSet.setFilter(n -> info.isSelected(n) || !NodeUtils.isFolder(n));
        }

        dataSet.addAll(nodes);
        if (dataSet.size() > 0) {
            rootView.post(() -> {
                if (this.contentLoadedListener != null) {
                    rootView.post(() -> this.contentLoadedListener.onContentLoaded());
                }
                emptyContentComponent.hide();
                this.swipeRefreshLayout.setVisibility(View.VISIBLE);
            });
        } else {
            rootView.post(() -> {
                if (this.emptyContentListener != null) {
                    rootView.post(() -> this.emptyContentListener.onEmptyContent());
                }

                if (state.node.type() != Node.TYPE_REMOTE_NODE && state.node.type() != Node.TYPE_WORKSPACE) {
                    emptyContentComponent.setButtonText(state.activityContext.getString(R.string.refresh));
                    emptyContentComponent.setIcon(R.drawable.search);
                } else {
                    emptyContentComponent.setButtonText(state.activityContext.getString(R.string.add));
                    emptyContentComponent.setIcon(R.drawable.ic_folder_outline_grey600_48dp);
                }
                this.swipeRefreshLayout.setVisibility(View.GONE);
                emptyContentComponent.hideActionButton();
                emptyContentComponent.show();

            });
        }
        updateView();
    }

    @Override
    public void setInForeground() {
        this.state.guiContext.refreshActionBar();
        super.setInForeground();
    }

    private void updateView() {
        rootView.post(() -> {
            int mode = this.state.displayInfo.mode();
            Metrics m = this.state.metrics;
            m.calculateItemsWidth(getWidth());
            int column = m.columnCount(mode);
            this.layoutManager = new GridLayoutManager(this.state.activityContext, column);
            this.recyclerView.setLayoutManager(layoutManager);
            this.adapter.update(dataSet);
            this.swipeRefreshLayout.setRefreshing(false);
        });
    }

    public void onEmptyContentActionTriggered(View v) {
        this.refresh();
    }

    private void finishRefreshing() {
        if (this.swipeRefreshLayout.isRefreshing()) {
            rootView.post(() -> this.swipeRefreshLayout.setRefreshing(false));
        }
    }

    private void startRefreshing() {
        rootView.post(() -> this.swipeRefreshLayout.setRefreshing(true));
    }

    @Override
    public void onCreated(Node... nodes) {

    }

    @Override
    public void onDeleted(Node... nodes) {

    }

    @Override
    public void onUpdated(Node... nodes) {

    }

    // EventHandler
    @Override
    public void onRenamed(Node node, String newName) {

    }

    @Override
    public void onShared(Node node) {

    }

    @Override
    public void onUnShared(Node node) {

    }

    @Override
    public void onBookmarked(Node node) {

    }

    @Override
    public void onUnBookmarked(Node node) {

    }

    @Override
    public void onWatched(Node node) {

    }

    @Override
    public void onUnWatched(Node node) {

    }
}

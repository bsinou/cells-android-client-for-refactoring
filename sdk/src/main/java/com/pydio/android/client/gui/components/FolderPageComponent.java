package com.pydio.android.client.gui.components;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pydio.android.client.R;
import com.pydio.android.client.accounts.Accounts;
import com.pydio.android.client.accounts.AuthenticationEventHandler;
import com.pydio.android.client.data.Display;
import com.pydio.android.client.data.PydioAgent;
import com.pydio.android.client.data.Session;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.auth.jwt.JWT;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.data.images.ThumbLoader;
import com.pydio.android.client.data.listing.ContentPageState;
import com.pydio.android.client.data.listing.NodeDataSet;
import com.pydio.android.client.data.nodes.EventHandler;
import com.pydio.android.client.data.nodes.NodeUtils;
import com.pydio.android.client.data.nodes.OfflineInfo;
import com.pydio.android.client.data.nodes.SelectionInfo;
import com.pydio.android.client.gui.adapters.NodeListAdapter;
import com.pydio.android.client.gui.view.NodeViewTag;
import com.pydio.android.client.gui.view.ViewDataBinder;
import com.pydio.android.client.gui.view.ViewWrapper;
import com.pydio.android.client.gui.view.group.Metrics;
import com.pydio.android.client.gui.view.ViewData;
import com.pydio.android.client.services.Cache;
import com.pydio.android.client.utils.Background;
import com.pydio.android.client.utils.Task;
import com.pydio.android.client.utils.Threading;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.auth.OauthConfig;
import com.pydio.sdk.core.auth.Token;
import com.pydio.sdk.core.common.errors.Code;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.utils.Log;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FolderPageComponent extends BrowserPage
        implements AuthenticationEventHandler, EventHandler, NodeListAdapter.ImageThumbLoader, ViewDataBinder {

    final private Object syncToken = 0;

    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout rootView;
    private ContentPageState state;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private EmptyContentComponent emptyContentComponent;

    private NodeDataSet dataSet;
    private PydioAgent agent;
    private String label;
    private NodeListAdapter adapter;
    private int displayMode;

    private Map<ImageView, String> thumbsRequests;
    private Map<String, Bitmap> thumbsMemoryCache;
    private final Object thumbLock = 0;

    private Task pollTask;
    private long interval = 500;
    private boolean stopRequested;

    private Map<String, String> renamedEvents;

    public FolderPageComponent(ContentPageState data) {
        this.state = data;
        this.renamedEvents = new HashMap<>();

        LayoutInflater inflater = LayoutInflater.from(data.activityContext);
        this.rootView = (FrameLayout) inflater.inflate(R.layout.view_swipe_refresh_grid_layout, null, false);
        this.swipeRefreshLayout = rootView.findViewById(R.id.swipe_layout);

        this.swipeRefreshLayout.setOnRefreshListener(this::reload);
        this.recyclerView = rootView.findViewById(R.id.gridView);

        this.displayMode = this.state.displayInfo.mode();

        this.agent = new PydioAgent(data.session);
        this.label = data.node.label();

        View view = this.rootView.findViewById(R.id.empty_list_layout);
        this.emptyContentComponent = new EmptyContentComponent(view);
        this.emptyContentComponent.setButtonClickListener(this::emptyContentActionClickedListener);

        this.thumbsRequests = new HashMap<>();
        this.thumbsMemoryCache = new HashMap<>();

        this.adapter = new NodeListAdapter(this);
        this.recyclerView.setAdapter(adapter);
    }

    @Override
    public View getView() {
        return rootView;
    }

    // Dimensions
    public void setHeight(int height) {
        swipeRefreshLayout.getLayoutParams().height = height;
        recyclerView.getLayoutParams().height = height + 300;
        swipeRefreshLayout.requestLayout();
        recyclerView.requestLayout();
        super.setHeight(height);
    }

    public void setWidth(int width) {
        recyclerView.getLayoutParams().width = GridLayout.LayoutParams.MATCH_PARENT;
        super.setWidth(width);
    }

    // Utils
    private String workspaceSlug() {
        Node node = state.node;
        if (node.type() == Node.TYPE_WORKSPACE) {
            return node.id();
        } else {
            return node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
        }
    }

    private boolean targetingCellsVersion() {
        return this.state.session.server.versionName().contains("cells");
    }

    // Errors
    private void onError(Error error) {
        final int code = error.code;
        if (code == Code.authentication_required || code == Code.authentication_with_captcha_required) {
            state.guiContext.onAuthenticationRequired(this::reload);
        }
    }

    // Data
    public void refresh() {
        reload();
    }

    @Override
    public void displayModeChanged() {
        int mode = this.state.displayInfo.mode();
        Metrics m = this.state.metrics;
        int width = getWidth();
        m.calculateItemsWidth(width);
        int column = m.columnCount(mode);
        layoutManager = new GridLayoutManager(this.state.activityContext, column);

        this.recyclerView.setLayoutManager(layoutManager);
        this.adapter = new NodeListAdapter(this);
        this.recyclerView.setAdapter(adapter);
        this.adapter.update(dataSet);
        this.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void loadContent() {
        this.state.guiContext.setTitle(this.label);
        load();
    }

    private void load() {
        int type = state.node.type();
        if (type == Node.TYPE_BOOKMARKS) {
            startRefreshing();
            this.agent.getBookmarks((nodes, error) -> {
                finishRefreshing();
                if (error != null) {
                    showMessage("failed to list bookmarks: " + error.toString());
                    return;
                }
                onLoaded(nodes);
            });
        } else {
            this.agent.ls(state.node, true, 500, 0, (nodes, error) -> {
                if (error != null) {
                    showMessage(state.activityContext.getString(R.string.failed_to_get_folder_content), label);
                    return;
                }
                if (nodes.size() == 0) {
                    reload();

                } else {
                    onLoaded(nodes);
                }
            });
        }
    }

    private void reload() {
        int type = state.node.type();
        if (type == Node.TYPE_BOOKMARKS || type == Node.TYPE_SEARCH) {
            load();
            return;
        }

        startRefreshing();
        synchronized (thumbLock) {
            thumbsRequests.clear();
            thumbsMemoryCache.clear();
        }

        String slug;
        if (state.node.type() == Node.TYPE_WORKSPACE) {
            slug = state.node.id();
        } else {
            slug = state.node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
        }

        agent.ls(state.node, false, 0, 1000, (nodes, error) -> {
            if (error != null) {
                finishRefreshing();
                onError(error);
                return;
            }

            String cacheWorkspace = this.state.session.workspaceCacheID(slug);
            Cache.clear(cacheWorkspace, state.node.path());

            for (Node n : nodes) {
                long id = Cache.addNode(cacheWorkspace, state.node.path(), n);
                n.setProperty(Pydio.NODE_PROPERTY_ID, String.valueOf(id));
            }
            onLoaded(nodes);
        });
    }

    private void onLoaded(List<Node> nodes) {
        if (dataSet == null) {
            dataSet = new NodeDataSet();
            dataSet.setSorter(this.state.sorter);
            if (state.mode == ModePaste) {
                final SelectionInfo info = this.state.guiContext.getSelectionInfo();
                dataSet.setFilter(n -> info.isSelected(n) || !NodeUtils.isFolder(n));
            }
        } else {
            dataSet.clear();
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
                emptyContentComponent.show();
                if (NodeUtils.isRecycleBin(this.state.node)) {
                    emptyContentComponent.hideActionButton();
                }
            });
        }
        updateView();
    }

    @Override
    public int count() {
        if (dataSet == null) {
            return 0;
        }
        return dataSet.size();
    }

    @Override
    public Node node() {
        return state.node;
    }

    private String cacheWorkspace(Node node) {
        final int type = node.type();

        if (type == Node.TYPE_WORKSPACE) {
            return this.state.session.workspaceCacheID(node.id());
        } else if (type == Node.TYPE_REMOTE_NODE) {
            return this.state.session.workspaceCacheID(node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG));
        } else if (type == Node.TYPE_SEARCH) {
            return this.state.session.id() + ":search";
        } else if (type == Node.TYPE_BOOKMARKS) {
            return this.state.session.id() + ":bookmarks";
        }
        return "";
    }

    @Override
    public int mode() {
        return state.mode;
    }

    // Events
    @Override
    public List<Node> get(com.pydio.android.client.data.listing.Filter filter) {
        return dataSet.filter(filter);
    }

    @Override
    public Node itemAt(int index) {
        if (dataSet == null) {
            return null;
        }
        return dataSet.get(index);
    }

    @Override
    public void enterSelectionMode() {
        this.swipeRefreshLayout.setRefreshing(false);
        this.swipeRefreshLayout.setEnabled(false);
        for (int i = 0; i < this.recyclerView.getChildCount(); i++) {
            View v = this.recyclerView.getChildAt(i);
            updateViewForSelection(v);
        }
    }

    @Override
    public void exitSelectionMode() {
        this.swipeRefreshLayout.setRefreshing(false);
        this.swipeRefreshLayout.setEnabled(true);
        this.state.guiContext.setTitle(this.label);
        for (int i = 0; i < this.recyclerView.getChildCount(); i++) {
            View v = this.recyclerView.getChildAt(i);
            updateViewForSelection(v);
        }
    }

    @Override
    public void enterPasteMode() {

    }

    @Override
    public void exitPasteMode() {

    }

    @Override
    public void setInBackground() {
        stopRequested = true;
        if (pollTask != null) {
            pollTask.cancel();
        }
    }

    @Override
    public void setInForeground() {
        this.state.guiContext.setTitle(this.label);
        this.rootView.setVisibility(View.VISIBLE);
        if (dataSet.size() > 0) {
            if (this.contentLoadedListener != null) {
                this.contentLoadedListener.onContentLoaded();
            }
            if (this.displayMode != this.state.displayInfo.mode()) {
                this.displayMode = this.state.displayInfo.mode();
                updateView();
            }
        } else {
            if (this.emptyContentListener != null) {
                this.emptyContentListener.onEmptyContent();
            }
        }

        if (!this.state.session.server.versionName().contains("cells")) {
            return;
        }

        stopRequested = true;
        if (pollTask != null) {
            pollTask.cancel();
        }
        pollTask = Background.go(this::poll);
    }

    // Events handling
    private void updateView() {
        rootView.post(() -> {
            int mode = this.state.displayInfo.mode();
            Metrics m = this.state.metrics;
            m.calculateItemsWidth(getWidth());
            int column = m.columnCount(mode);
            layoutManager = new GridLayoutManager(this.state.activityContext, column);
            this.recyclerView.setLayoutManager(layoutManager);
            this.adapter.update(dataSet);
            this.swipeRefreshLayout.setRefreshing(false);
            if (pollTask == null || pollTask.taskDone()) {
                pollTask = Background.go(this::poll);
            }
        });
    }

    private void finishRefreshing() {
        if (this.swipeRefreshLayout.isRefreshing()) {
            rootView.post(() -> this.swipeRefreshLayout.setRefreshing(false));
        }
    }

    private void startRefreshing() {
        rootView.post(() -> this.swipeRefreshLayout.setRefreshing(true));
    }

    private void emptyContentActionClickedListener(View v) {
        if (state.node.type() != Node.TYPE_REMOTE_NODE && state.node.type() != Node.TYPE_WORKSPACE) {
            reload();
            return;
        }

        if (this.emptyContentActionHandler != null) {
            this.emptyContentActionHandler.onAction();
        }

    }

    @Override
    public void loadBitmap(ImageView image, Node node, int dim) {
        if (node == null) {
            synchronized (thumbLock) {
                thumbsRequests.remove(image);
                return;
            }
        }

        final String label = node.label();

        synchronized (thumbLock) {
            Bitmap bitmap = thumbsMemoryCache.get(label);
            if (bitmap != null) {
                Drawable d = image.getDrawable();
                image.setAdjustViewBounds(true);
                image.setColorFilter(Color.parseColor("#00000000"));
                image.setScaleX(1);
                image.setScaleY(1);
                image.setImageBitmap(bitmap);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (image.getDrawable() == null) {
                    image.setImageDrawable(d);
                }
                return;
            }
            thumbsRequests.put(image, node.label());
        }

        ThumbLoader loader = this.state.guiContext.thumbLoader();
        if (loader != null) {
            loader.loadThumb(node, dim, (b) -> {
                synchronized (thumbLock) {
                    thumbsMemoryCache.put(node.label(), b);
                    String savedLabel = thumbsRequests.get(image);
                    if (label.equals(savedLabel)) {
                        rootView.post(() -> {
                            Drawable d = image.getDrawable();
                            image.setAdjustViewBounds(true);
                            image.setColorFilter(Color.parseColor("#00000000"));
                            image.setScaleX(1);
                            image.setScaleY(1);
                            image.setImageBitmap(b);
                            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            if (image.getDrawable() == null) {
                                image.setImageDrawable(d);
                            }
                        });
                    }
                    thumbsRequests.remove(image);
                }
            });
        }
    }

    @Override
    public View createView(int type) {
        return new ViewWrapper(null, this.state.displayInfo.mode(), this.state.metrics).getView();
    }

    @Override
    public void bindData(View view, int position) {
        int display = this.state.displayInfo.mode();
        ViewWrapper wrapper = new ViewWrapper(view, this.state.displayInfo.mode(), this.state.metrics);

        SelectionInfo info = this.state.guiContext.getSelectionInfo();
        OfflineInfo offline = this.state.guiContext.getOfflineInfo();
        Context context = this.state.activityContext;

        final Node node = dataSet.get(position);
        ViewData data = ViewData.parse(context, node, offline, info);
        data.setOptionClickListener((v) -> this.onOptionClick(view, node));
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
        itemView.setOnClickListener((v) -> this.onClick(view, node));
        itemView.setOnLongClickListener(v -> {
            if (this.longClickListener != null) {
                this.onLongClick(itemView, node);
            }
            return true;
        });
        ImageView icon = itemView.findViewById(R.id.icon);
        this.loadBitmap(icon, null, 200);
        wrapper.refresh(this);
    }

    private void poll() {
        if (state.mode != ModeDefault) {
            return;
        }

        if (!this.state.session.server.versionName().contains("cells")) {
            return;
        }
        if (this.state.node.type() != Node.TYPE_WORKSPACE && this.state.node.type() != Node.TYPE_REMOTE_NODE) {
            return;
        }

        stopRequested = false;
        String workspaceSlug = workspaceSlug();
        final PydioAgent agent = new PydioAgent(this.state.session);
        int remainingAttempts = 3;

        Context ctx = this.state.activityContext;
        final Client client = agent.client;

        while (this.pollTask != null && !stopRequested && !pollTask.taskDone() && remainingAttempts > 0) {
            remainingAttempts--;
            List<Node> currentList = this.dataSet.copy();
            List<Node> nodes = new ArrayList<>();

            try {
                client.ls(workspaceSlug, this.state.node.path(), nodes::add);

            } catch (SDKException e) {
                switch (e.code) {
                    case 401:
                    case Code.authentication_required:
                    case Code.authentication_with_captcha_required:
                        if (this.agent.supportOAuth()) {
                            this.stopRequested = true;
                            OauthConfig cfg = OauthConfig.fromJSON(this.agent.session.server.getOIDCInfo(), "");
                            Accounts.manager.authorize(cfg, this);
                            return;
                        }

                        if (e.code == Code.authentication_with_captcha_required) {
                            this.state.guiContext.onAuthenticationRequired(FolderPageComponent.this::poll);
                            return;
                        }

                        Database.deleteToken(state.session.tokenKey());
                        continue;

                    case Code.unreachable_host:
                    case Code.con_failed:
                    case Code.con_closed:
                    case Code.no_internet:
                        FolderPageComponent.this.showMessage(ctx.getString(R.string.could_not_reach_server));
                        Threading.sleep(5000);
                        continue;

                    case Code.ssl_error:
                    case Code.tls_init:
                        FolderPageComponent.this.showMessage(
                                ctx.getString(R.string.problem_with_server_cert) + " " + e.getLocalizedMessage());
                        return;

                    default:
                        String message = e.getMessage();
                        if (message != null) {
                            FolderPageComponent.this.showMessage(
                                    ctx.getString(R.string.an_error_occurred) + " " + e.getLocalizedMessage());
                        }
                        return;
                }
            }

            if (stopRequested || pollTask.taskDone()) {
                return;
            }

            final ArrayList<Node> deleted = new ArrayList<>();
            final ArrayList<Node> updated = new ArrayList<>();
            final ArrayList<Node> added = new ArrayList<>();
            final ArrayList<Node> old = new ArrayList<>();

            if (nodes.size() > 0) {
                for (Node n : nodes) {
                    if (stopRequested || pollTask.taskDone()) {
                        return;
                    }

                    boolean found = false;
                    for (Node o : currentList) {
                        int compare = o.compare(n);
                        if (compare == Node.same) {
                            found = true;
                            old.add(n);
                            break;
                        }
                        if (compare == Node.content) {
                            found = true;
                            updated.add(n);
                            break;
                        }
                    }
                    if (!found) {
                        added.add(n);
                    }
                }

                if (stopRequested || pollTask.taskDone()) {
                    return;
                }

                ArrayList<Node> newDataSet = new ArrayList<>();
                newDataSet.addAll(old);
                newDataSet.addAll(added);
                newDataSet.addAll(updated);

                for (Node n : currentList) {
                    if (stopRequested) {
                        return;
                    }
                    if (!newDataSet.contains(n)) {
                        deleted.add(n);
                    }
                }

            } else {
                deleted.addAll(currentList);
            }

            if (stopRequested || pollTask.taskDone()) {
                return;
            }

            if ((deleted.size() > 0 || updated.size() > 0 || added.size() > 0)) {
                if (renamedEvents.size() > 0 && added.size() > 0) {
                    Iterator<Node> cit = added.iterator();
                    Iterator<Node> dit = deleted.iterator();

                    while (cit.hasNext()) {
                        Node c = cit.next();
                        String cPath = c.path();
                        if (renamedEvents.containsKey(cPath)) {
                            boolean found = false;
                            String oldPath = renamedEvents.get(cPath);
                            while (dit.hasNext()) {
                                Node d = dit.next();
                                if (d.path().equals(oldPath)) {
                                    found = true;
                                    dit.remove();
                                    break;
                                }
                            }
                            if (found) {
                                cit.remove();
                                c.setProperty(Pydio.NODE_PROPERTY_ORIGINAL_PATH, oldPath);
                                updated.add(c);
                            }
                        }
                    }
                }

                this.onDeleted(deleted.toArray(new Node[] {}));
                this.onUpdated(updated.toArray(new Node[] {}));
                this.onCreated(added.toArray(new Node[] {}));
            }
            Threading.sleep(interval);
            interval = 1500;
        }
    }

    private void onClick(View view, Node node) {
        if (this.clickListener != null) {
            this.clickListener.onClick(node);
        }

        SelectionInfo info = this.state.guiContext.getSelectionInfo();
        if (info.inSelectionMode()) {
            updateViewForSelection(view);
        }
    }

    private void onLongClick(View view, Node node) {
        if (this.longClickListener != null) {
            this.longClickListener.onLongClick(node);
        }
        updateViewForSelection(view);
    }

    private void onOptionClick(View view, Node node) {
        SelectionInfo info = this.state.guiContext.getSelectionInfo();
        if (info.inSelectionMode()) {
            this.onClick(view, node);
            return;
        }

        if (this.itemOptionClickedListener != null) {
            this.itemOptionClickedListener.onClick(node);
        }
    }

    private void updateViewForSelection(View view) {
        NodeViewTag tag = (NodeViewTag) view.getTag();

        SelectionInfo info = this.state.guiContext.getSelectionInfo();
        ViewWrapper wrapper;
        wrapper = ViewWrapper.wrap(view);

        tag.data.setSelectionMode(info.inSelectionMode());
        tag.data.setSelected(info.isSelected(tag.node));
        wrapper.setData(tag.data);

        wrapper.refreshedSelected();
    }

    // EventHandler
    @Override
    public void onCreated(Node... nodes) {
        synchronized (syncToken) {
            int initialDataSetSize = this.dataSet.size();
            int index = -1;
            int addedCount = 0;
            for (Node node : nodes) {
                int addedIndex = dataSet.add(node);
                if (addedIndex != -1) {
                    Cache.addNode(cacheWorkspace(node), state.node.path(), node);

                    addedCount++;
                    if (index == -1) {
                        index = addedIndex;
                    } else {
                        if (addedIndex < index) {
                            index = addedIndex;
                        }
                    }
                }
            }
            if (index != -1) {
                int finalAddedCount = addedCount;
                int finalIndex = index;
                if (addedCount == 1) {
                    this.rootView.post(() -> this.adapter.notifyItemInserted(finalIndex));
                } else {
                    this.rootView.post(() -> this.adapter.notifyItemRangeInserted(finalIndex, finalAddedCount));
                }

                if (initialDataSetSize == 0) {
                    rootView.post(() -> {
                        if (this.contentLoadedListener != null) {
                            this.contentLoadedListener.onContentLoaded();
                        }
                        emptyContentComponent.hide();
                        this.swipeRefreshLayout.setVisibility(View.VISIBLE);
                    });
                }
            }
        }
    }

    @Override
    public void onDeleted(Node... nodes) {
        synchronized (syncToken) {
            int index = -1;
            int deleteCount = 0;
            for (Node node : nodes) {
                int removedIndex = dataSet.indexOf(node);
                if (removedIndex != -1) {
                    deleteCount++;
                    if (index == -1) {
                        index = removedIndex;

                    } else {
                        if (removedIndex < index) {
                            index = removedIndex;
                        }
                    }
                }
            }
            for (Node node : nodes) {
                dataSet.remove(node);
                Cache.deleteNode(cacheWorkspace(node), state.node.path(), node.label());
            }
            if (index != -1) {
                int finalAddedCount = deleteCount;
                int finalIndex = index;
                if (deleteCount == 1) {
                    this.rootView.post(() -> this.adapter.notifyItemRemoved(finalIndex));
                } else {
                    this.rootView.post(() -> this.adapter.notifyItemRangeRemoved(finalIndex, finalAddedCount));
                }
                if (dataSet.size() == 0) {
                    onLoaded(new ArrayList<>());
                }
            }
        }
    }

    @Override
    public void onUpdated(Node... nodes) {
        List<Node> createEvents = new ArrayList<>();
        List<Node> deleteEvents = new ArrayList<>();
        synchronized (syncToken) {
            int index = -1;
            int updatedCount = 0;

            for (Node node : nodes) {
                int updatedIndex = dataSet.indexOf(node);

                if (updatedIndex != -1) {
                    dataSet.update(node, node);
                    Cache.update(cacheWorkspace(node), this.state.node.path(), node.label(), node);

                } else {
                    // might be on targeting a p8 server
                    String originalPath = node.getProperty("original_path");
                    String nodePath = node.path();

                    if (nodePath.equals(originalPath)) {
                        // edit event
                        updatedIndex = this.dataSet.indexOf(node);
                        this.dataSet.update(node, node);
                        Cache.update(cacheWorkspace(node), this.state.node.path(), node.label(), node);

                    } else {
                        String nodeParent = new File(nodePath).getParent();
                        String originalParent = new File(originalPath).getParent();
                        String originalName = new File(originalPath).getName();

                        if (nodeParent.equals(this.state.node.path())) {
                            if (originalParent.equals(this.state.node.path())) {
                                // rename event
                                Node oldNode = this.dataSet.findByProperty(Pydio.NODE_PROPERTY_FILENAME, originalPath);
                                updatedIndex = this.dataSet.update(oldNode, node);
                                Cache.update(cacheWorkspace(node), originalParent, originalName, node);
                            } else {
                                // create event
                                createEvents.add(node);
                            }
                        } else if (originalParent.equals(this.state.node.path())) {
                            node.setProperty(Pydio.NODE_PROPERTY_FILENAME, originalPath);
                            deleteEvents.add(node);
                        }
                    }
                }

                if (updatedIndex != -1) {
                    updatedCount++;
                    if (index == -1) {
                        index = updatedIndex;

                    } else {
                        if (updatedIndex < index) {
                            index = updatedIndex;
                        }
                    }
                }
            }

            if (index != -1) {
                int finalAddedCount = updatedCount;
                int finalIndex = index;
                if (updatedCount == 1) {
                    this.rootView.post(() -> this.adapter.notifyItemChanged(finalIndex));
                } else {
                    this.rootView.post(() -> this.adapter.notifyItemRangeChanged(finalIndex, finalAddedCount));
                }
            }
        }
        if (deleteEvents.size() > 0) {
            Node[] deleted = new Node[deleteEvents.size()];
            for (int i = 0; i < deleteEvents.size(); i++) {
                deleted[i] = deleteEvents.get(i);
            }
            this.onDeleted(deleted);
        }
        if (createEvents.size() > 0) {
            Node[] created = new Node[createEvents.size()];
            for (int i = 0; i < deleteEvents.size(); i++) {
                created[i] = createEvents.get(i);
            }
            this.onCreated(created);
        }
    }

    @Override
    public void onRenamed(Node node, String newName) {
        String newPath = (new File(node.path()).getParent() + "/" + newName).replace("//", "/");
        String oldPath = node.path();
        if (this.targetingCellsVersion()) {
            this.renamedEvents.put(newPath, oldPath);
        } else {
            node.setProperty(Pydio.NODE_PROPERTY_ORIGINAL_PATH, oldPath);
            node.setProperty(Pydio.NODE_PROPERTY_FILENAME, newPath);
            this.onUpdated(node);
        }
    }

    @Override
    public void onShared(Node node) {
        synchronized (this.syncToken) {
            if (!this.targetingCellsVersion()) {
                this.dataSet.update(node, node);
                Cache.update(cacheWorkspace(node), this.state.node.path(), node.label(), node);

                View view = getNodeView(node);
                if (view == null) {
                    return;
                }

                NodeViewTag tag = (NodeViewTag) view.getTag();
                tag.data.setShared(true);
                this.rootView.post(() -> {
                    ViewWrapper wrapper = ViewWrapper.wrap(view);
                    wrapper.setData(tag.data);
                    wrapper.refreshShared();
                });
            }
        }
    }

    @Override
    public void onUnShared(Node node) {
        synchronized (this.syncToken) {
            if (!this.targetingCellsVersion()) {
                this.dataSet.update(node, node);
                Cache.update(cacheWorkspace(node), this.state.node.path(), node.label(), node);

                View view = getNodeView(node);
                if (view == null) {
                    return;
                }

                NodeViewTag tag = (NodeViewTag) view.getTag();
                tag.data.setShared(false);
                this.rootView.post(() -> {
                    ViewWrapper wrapper = ViewWrapper.wrap(view);
                    wrapper.setData(tag.data);
                    wrapper.refreshShared();
                });
            }
        }
    }

    @Override
    public void onBookmarked(Node node) {
        if (!targetingCellsVersion()) {
            synchronized (syncToken) {
                this.dataSet.update(node, node);
                Cache.update(cacheWorkspace(node), this.state.node.path(), node.label(), node);

                View view = getNodeView(node);
                if (view == null) {
                    return;
                }
                NodeViewTag tag = (NodeViewTag) view.getTag();
                tag.data.setStarred(true);
                this.rootView.post(() -> {
                    ViewWrapper wrapper = ViewWrapper.wrap(view);
                    wrapper.setData(tag.data);
                    wrapper.refreshStarred();
                });
            }
        }
    }

    @Override
    public void onUnBookmarked(Node node) {
        if (!targetingCellsVersion()) {
            synchronized (syncToken) {
                this.dataSet.update(node, node);
                Cache.update(cacheWorkspace(node), this.state.node.path(), node.label(), node);
                View view = getNodeView(node);
                if (view == null) {
                    return;
                }
                NodeViewTag tag = (NodeViewTag) view.getTag();
                tag.data.setStarred(false);
                this.rootView.post(() -> {
                    ViewWrapper wrapper = ViewWrapper.wrap(view);
                    wrapper.setData(tag.data);
                    wrapper.refreshStarred();
                });
            }
        }
    }

    @Override
    public void onWatched(Node node) {
        synchronized (syncToken) {
            View view = getNodeView(node);
            if (view == null) {
                return;
            }

            NodeViewTag tag = (NodeViewTag) view.getTag();
            tag.data.setIsSynced(true);
            this.rootView.post(() -> {
                ViewWrapper wrapper = ViewWrapper.wrap(view);
                wrapper.setData(tag.data);
                wrapper.refreshSynced();
            });
        }
    }

    @Override
    public void onUnWatched(Node node) {
        synchronized (syncToken) {
            View view = getNodeView(node);
            if (view == null) {
                return;
            }

            NodeViewTag tag = (NodeViewTag) view.getTag();
            tag.data.setIsSynced(false);
            this.rootView.post(() -> {
                ViewWrapper wrapper = ViewWrapper.wrap(view);
                wrapper.setData(tag.data);
                wrapper.refreshSynced();
            });
        }
    }

    private View getNodeView(Node node) {
        for (int i = 0; i < this.recyclerView.getChildCount(); i++) {
            View v = this.recyclerView.getChildAt(i);
            NodeViewTag t = (NodeViewTag) v.getTag();
            if (t.node.compare(node) != FileNode.different) {
                return v;
            }
        }
        return null;
    }

    // Oauth get token handler

    @Override
    public void onError(String error, String description) {
        showMessage(error);
    }

    @Override
    public void handleToken(String stringToken) throws IOException {
        Token t;
        try {
            t = Token.decodeOauthJWT(stringToken);
        } catch (ParseException e) {
            e.printStackTrace();
            showMessage(this.state.activityContext.getString(R.string.could_not_get_token));
            return;
        }

        JWT jwt = JWT.parse(t.idToken);
        if (jwt == null) {
            showMessage(this.state.activityContext.getString(R.string.could_not_decode_id_token));
            return;
        }

        t.expiry = System.currentTimeMillis() / 1000 + t.expiry;
        String url = this.agent.session.server.url();

        t.subject = String.format("%s@%s", jwt.claims.name, url);
        Database.saveToken(t);

        Session session = new Session();
        session.user = jwt.claims.name;
        session.server = this.agent.session.server;
        this.state.guiContext.sessionUpdated(session);
    }

    @Override
    public void startIntent(Intent intent) {
        this.state.activityContext.startActivity(intent);
    }
}

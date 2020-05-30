package com.pydio.android.client.gui.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.navigation.NavigationView;
import com.pydio.android.client.R;
import com.pydio.android.client.accounts.AuthenticationEventHandler;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.GUIContext;
import com.pydio.android.client.data.Connectivity;
import com.pydio.android.client.data.ContentResolver;
import com.pydio.android.client.data.Display;
import com.pydio.android.client.data.LocalFS;
import com.pydio.android.client.data.PreviewerData;
import com.pydio.android.client.data.PydioAgent;
import com.pydio.android.client.data.Selection;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.State;
import com.pydio.sdk.core.auth.jwt.JWT;
import com.pydio.android.client.data.callback.Completion;
import com.pydio.android.client.data.callback.StringCompletion;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.data.files.FileUtils;
import com.pydio.android.client.data.images.ThumbLoader;
import com.pydio.android.client.data.images.ThumbStore;
import com.pydio.android.client.data.listing.ContentPageState;
import com.pydio.android.client.data.listing.Sorter;
import com.pydio.android.client.data.nodes.NodeUtils;
import com.pydio.android.client.data.nodes.OfflineInfo;
import com.pydio.android.client.data.nodes.SelectionInfo;
import com.pydio.android.client.data.transfers.Listener;
import com.pydio.android.client.data.transfers.Transfer;
import com.pydio.android.client.features.offline.EventsListener;
import com.pydio.android.client.gui.components.BrowserPage;
import com.pydio.android.client.gui.components.ConfirmDialogComponent;
import com.pydio.android.client.gui.components.DrawerAccountComponent;
import com.pydio.android.client.gui.components.InputTextDialogComponent;
import com.pydio.android.client.gui.components.ListItemMenuComponent;
import com.pydio.android.client.gui.components.LoginDialogComponent;
import com.pydio.android.client.gui.components.ProgressDialogComponent;
import com.pydio.android.client.gui.components.SessionListComponent;
import com.pydio.android.client.gui.components.StatusLayoutComponent;
import com.pydio.android.client.gui.components.TaskDialogComponent;
import com.pydio.android.client.gui.components.WorkspaceListComponent;
import com.pydio.android.client.gui.dialogs.models.DialogData;
import com.pydio.android.client.gui.dialogs.models.InputDialogData;
import com.pydio.android.client.gui.dialogs.models.ProgressDialogData;
import com.pydio.android.client.gui.dialogs.models.TaskDialogData;
import com.pydio.android.client.gui.menu.ListItemMenuData;
import com.pydio.android.client.gui.menu.models.ActionData;
import com.pydio.android.client.gui.view.group.DisplayConfig;
import com.pydio.android.client.gui.view.group.Metrics;
import com.pydio.android.client.gui.components.NodeListComponent;
import com.pydio.android.client.services.Cache;
import com.pydio.android.client.services.OfflineService;
import com.pydio.android.client.utils.Background;
import com.pydio.android.client.utils.Task;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.auth.OauthConfig;
import com.pydio.sdk.core.auth.Token;
import com.pydio.sdk.core.common.errors.Code;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.BookmarkNode;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Message;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.model.Stats;
import com.pydio.sdk.core.model.WorkspaceNode;
import com.pydio.sdk.core.server.Plugin;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Browser extends PydioDrawerActivity implements Listener, GUIContext, Display.Info, OfflineInfo, SelectionInfo, EventsListener, AuthenticationEventHandler {
    Metrics metrics;
    ThumbStore thumbStore;
    private TaskDialogComponent taskDialog;
    private String sorter;
    private Map<String, Sorter<FileNode>> sorters = new HashMap<>();
    private boolean sortOrderDesc;

    State state;
    Session session;
    int displayMode;
    PydioAgent agent;

    StatusLayoutComponent statusView;
    DrawerAccountComponent accountComponent;
    ListItemMenuComponent nodeMenuComponent;
    SessionListComponent sessionListComponent;
    WorkspaceListComponent workspaceListComponent;
    NodeListComponent listComponent;

    Menu leftPanelWorkspaceMenu;
    Menu leftPanelSessionMenu;

    NavigationView leftPanelDataNavigationView;

    boolean firstStart = true;
    boolean workspaceSelected = false;
    boolean ignoreACLs;

    String shareAction;
    String downloadAction;
    String renameAction;
    String deleteAction;
    String uploadAction;
    String mkdirAction;
    String moveAction;
    String copyAction;
    Runnable onOAuthCallback;

    private File tempCameraShot;

    private int contentHeight = -1;
    private final Object eventLock = 0;


    private boolean inSelectionMode, inPasteMode, inSearchMode;
    private int pasteBrowsingLevel, currentSearchPageLevel;
    Selection selection;

    // Activity class methods
    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        super.setContentView(R.layout.activity_browser_layout);

        Intent intent = getIntent();
        String stateString = intent.getStringExtra("state");
        state = State.parse(stateString);
        if (state != null) {
            session = Application.findSession(state.session);
        }

        if (state == null || session == null || session.server == null || session.user == null) {
            intent = new Intent(this, Application.newServerClass);
            startActivity(intent);
            this.finish();
            return;
        }

        agent = new PydioAgent(session);
        taskDialog = new TaskDialogComponent(this);

        shareAction = getString(R.string.tag_share);
        downloadAction = getString(R.string.tag_download);
        renameAction = getString(R.string.tag_rename);
        deleteAction = getString(R.string.tag_delete);
        uploadAction = getString(R.string.tag_upload);
        mkdirAction = getString(R.string.tag_mkdir);
        moveAction = getString(R.string.tag_move);
        copyAction = getString(R.string.tag_copy);

        metrics = DisplayConfig.getDefault(this).getMetrics();

        inSelectionMode = false;
        //selection = new Selection(this);

        currentSearchPageLevel = 0;

        // Application.
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        OfflineService.startSession(session);

        if (firstStart) {
            firstStart = false;
            currentSearchPageLevel = 0;
            if (session.server.workspaces == null || session.server.workspaces.size() == 0) {
                this.requestWorkspaceNodeList();

            } else if (state.workspace == null || "".equals(state.workspace)) {
                onNoWorkspaceSelected();

            } else {
                WorkspaceNode wn = session.server.getWorkspace(state.workspace);
                if (wn == null) {
                    onNoWorkspaceSelected();
                    return;
                }
                selectWorkspace(wn);
            }
        }
        Transfer.addTransferListener("browser", this);
        Background.go(Transfer::processQueue);
    }
    @Override
    public void onBackPressed() {
        if (nodeMenuComponent.isShowing()) {
            nodeMenuComponent.hide();
            return;
        }
        if (inPasteMode) {
            if (pasteBrowsingLevel == 1) {
                promptToExitPasteMode();
                return;
            }
            pasteBrowsingLevel--;
            if (listComponent.hasStackedView()) {
                listComponent.pop();
                return;
            }
            return;
        }
        if (inSelectionMode) {
            exitSelectionMode();
            return;
        }
        if(inSearchMode) {
            if (this.currentSearchPageLevel == 0) {
                inSearchMode = false;
                return;
            }

            this.currentSearchPageLevel--;
            if (listComponent.hasStackedView()) {
                listComponent.pop();
                return;
            }
            return;
        }
        if (listComponent.hasStackedView()) {
            listComponent.pop();
            return;
        }
        openLeftPanel();
    }

    @Override
    protected void hideFAB() {
        listComponent.setViewWithFAB(false);
        super.hideFAB();
    }
    @Override
    protected void showFAB() {
        if (nodeMenuComponent.isShowing()) {
            return;
        }
        listComponent.setViewWithFAB(true);
        super.showFAB();
    }
    @Override
    protected void onStop() {
        super.onStop();
        Transfer.stopConsuming();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Transfer.stopConsuming();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Background.go(Transfer::processQueue);
        listComponent.setInForeground();

        if (requestCode == IntentCode.storageSelection) {
            onItemSelectedForImports(resultCode, data);
            return;
        } else if (requestCode == IntentCode.cameraSelection) {
            onImageShotForUpload(resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == IntentCode.authorizeImportFromStorage) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectForImports();
            } else {
                showMessage(R.string.manually_grant_storage_access);
            }
        } else if (requestCode == IntentCode.authorizeImportFromCamera) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shootAndUpload();
            } else {
                showMessage(R.string.manually_grant_camera_access);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (displayMode == Display.list) {
            MenuItem displayItem = menu.add(R.string.grid);
            displayItem.setIcon(R.drawable.grid);

            Drawable newIcon = displayItem.getIcon();
            newIcon.mutate().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
            displayItem.setIcon(newIcon);
        } else {
            MenuItem displayItem = menu.add(R.string.list);
            displayItem.setIcon(R.drawable.list);

            Drawable newIcon = displayItem.getIcon();
            newIcon.mutate().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
            displayItem.setIcon(newIcon);
        }
        return true;
    }
    @Override
    protected void onLeftPanelOpened() {
        super.onLeftPanelOpened();
        if (state.workspace != null && !state.workspace.equals("")) {
            agent.workspaces((workspaceNodes, error) -> {
                if (error != null) {
                    //this.showMessage(error.text);
                    //todo: handle properly error code
                    return;
                }

                ArrayList<WorkspaceNode> workspacesToPoll = new ArrayList<>(workspaceNodes);
                OfflineService.pollChanges(workspacesToPoll);
                OfflineService.setEventsListener(this);
                Database.saveSession(session);
                getHandler().post(() -> {
                    leftPanelDataNavigationView = findViewById(R.id.drawer_workspace_navigation_view);
                    workspaceListComponent = new WorkspaceListComponent(leftPanelDataNavigationView, leftPanelWorkspaceMenu);
                    workspaceListComponent.setData(this.state.workspace, session.server.workspaces, session.server.versionName().contains("cells"), this::selectWorkspace);
                });
            });
        }
    }
    @Override
    protected void onLeftPanelClosed() {
        super.onLeftPanelClosed();
        if (this.state.workspace != null && !"".equals(this.state.workspace)) {
            workspaceListComponent.setCurrentWorkspace(this.state.workspace);
        }
        accountComponent.setOptionIcon(R.drawable.ic_account_switch_grey600_48dp);
    }
    @Override
    public void homeButtonClicked(View v) {
        if (inSearchMode) {
            exitSearchMode();
            return;
        }

        if (inPasteMode) {
            exitPasteMode();
            return;
        }

        if (inSelectionMode) {
            exitSelectionMode();
            return;
        }
        super.homeButtonClicked(v);
    }

    protected int getContentHeight() {
        if (contentHeight == -1) {
            int actionBarHeight = (int) getResources().getDimension(R.dimen.action_bar_height);
            contentHeight = Application.getContentHeight() - actionBarHeight;
        }
        return contentHeight;
    }


    // Components initialization
    private void initView() {
        initActionBar();
        initNodeList();
        initFAB();

        initLeftPanel();
        initMenuItem();
        initStatusView();
    }

    private void initActionBar() {
        setActionBarHomeIcon(R.drawable.menu);
    }

    private void initNodeList() {
        View v = findViewById(R.id.node_list_view);
        listComponent = new NodeListComponent(v);
        listComponent.setEmptyContentListener(this::emptyContentEvent);
        listComponent.setEmptyContentActionHandler(this::emptyContentAction);
        listComponent.setContentLoadedListener(this::contentLoadedEvent);
        listComponent.setItemClickListener(this::itemClicked);
        listComponent.setItemOptionsClickListener(this::itemSecondaryActionClicked);
        listComponent.setItemLongClickListener(this::itemLongClicked);
        listComponent.setViewWithFAB(true);

        listComponent.setHeight(getContentHeight());

        displayMode = Display.list;
        String dm = Application.getPreference(Application.PREF_DISPLAY_MODE);
        if (!"".equals(dm)) {
            displayMode = Integer.parseInt(dm);
        }

        String sortCriteria = Application.getPreference(Application.PREF_SORT_CRITERIA);
        if ("".equals(sortCriteria)) {
            sortCriteria = Application.context().getResources().getString(R.string.tag_type);
        }

        sorter = sortCriteria;

        sortOrderDesc = Boolean.parseBoolean(Application.getPreference(Application.PREF_SORT_ORDER));
        sorters = new HashMap<>();
        sorters.put(getResources().getString(R.string.tag_size), (n1, n2) -> {
            if (n1.path().toLowerCase().equals("/recycle_bin")) return false;
            if (n2.path().toLowerCase().equals("/recycle_bin")) return true;
            if (sortOrderDesc) {
                return n1.size() < n2.size();
            }
            return n1.size() > n2.size();
        });
        sorters.put(getResources().getString(R.string.tag_type), (n1, n2) -> {
            if (n1.path().toLowerCase().equals("/recycle_bin")) return false;
            if (n2.path().toLowerCase().equals("/recycle_bin")) return true;

            if (n1.isFile() && !n2.isFile()) {
                return sortOrderDesc;
            }
            if (!n1.isFile() && n2.isFile()) {
                return !sortOrderDesc;
            }
            return n1.label().compareToIgnoreCase(n2.label()) <= 0;
        });
        sorters.put(getResources().getString(R.string.tag_filename), (n1, n2) -> {
            if (n1.path().toLowerCase().equals("/recycle_bin")) return false;
            if (n2.path().toLowerCase().equals("/recycle_bin")) return true;
            if (sortOrderDesc) {
                return n1.label().compareToIgnoreCase(n2.label()) >= 0;
            }
            return n1.label().compareToIgnoreCase(n2.label()) <= 0;
        });
        sorters.put(getResources().getString(R.string.tag_modified), (n1, n2) -> {
            if (n1.path().toLowerCase().equals("/recycle_bin")) return false;
            if (n2.path().toLowerCase().equals("/recycle_bin")) return true;
            if (sortOrderDesc) {
                return n1.lastModified() >= n2.lastModified();
            }
            return n1.lastModified() < n2.lastModified();
        });
    }

    private void initStatusView() {
        View v = findViewById(R.id.status_layout_view);
        statusView = new StatusLayoutComponent(v);
        v.setVisibility(View.GONE);
        statusView.setOnHideListener(() -> listComponent.getView().setVisibility(View.VISIBLE));
        statusView.setOnShowListener(() -> listComponent.getView().setVisibility(View.GONE));
    }

    private void initLeftPanel() {

        if (sessionsNavigationView != null) {
            leftPanelSessionMenu = sessionsNavigationView.getMenu();
            sessionsNavigationView.setVisibility(View.GONE);
        }
        dataNavigationView.setVisibility(View.VISIBLE);
        leftPanelWorkspaceMenu = dataNavigationView.getMenu();

        initPanelAccount();
        initWorkspaces();
        initSessions();

        MenuItem menuItem = leftPanelWorkspaceMenu.findItem(R.id.bookmarks);
        menuItem.setOnMenuItemClickListener((v) -> {
            Browser.this.closeLeftPanel();
            browseBookmarks();
            return true;
        });
        if (Application.customTheme() != null) {
            SpannableString spanString = new SpannableString(menuItem.getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Application.customTheme().getMainColor()), 0, spanString.length(), 0);
            menuItem.setTitle(spanString);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                menuItem.setIconTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
            }
            Drawable d = menuItem.getIcon();
            if (d != null) {
                d.mutate().setColorFilter(Application.customTheme().getMainColor(), PorterDuff.Mode.SRC_IN);
                menuItem.setIcon(d);
            }
        }

        /*menuItem = leftPanelWorkspaceMenu.findItem(R.id.recent);
        menuItem.setOnMenuItemClickListener((v) -> {
            Browser.this.closeLeftPanel();
            return true;
        });*/

        menuItem = leftPanelWorkspaceMenu.findItem(R.id.settings);
        menuItem.setOnMenuItemClickListener((v) -> {
            Intent intent = new Intent(Browser.this, Settings.class);
            Browser.this.startActivity(intent);
            return true;
        });
        if (Application.customTheme() != null) {
            SpannableString spanString = new SpannableString(menuItem.getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Application.customTheme().getMainColor()), 0, spanString.length(), 0);
            menuItem.setTitle(spanString);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                menuItem.setIconTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
            }
            Drawable d = menuItem.getIcon();
            if (d != null) {
                d.mutate().setColorFilter(Application.customTheme().getMainColor(), PorterDuff.Mode.SRC_IN);
                menuItem.setIcon(d);
            }
        }

        menuItem = leftPanelWorkspaceMenu.findItem(R.id.clear_cache);
        menuItem.setOnMenuItemClickListener((v) -> {
            if (state.workspace != null && !"".equals(state.workspace)) {
                Background.go(() -> {
                    String cacheWorkspace = session.workspaceCacheID(state.workspace);
                    Application.localSystem.deleteDirectory(new File(session.cacheFolderPath()));
                    Cache.clear(cacheWorkspace);
                    getHandler().post(() -> {
                        closeLeftPanel();
                        listComponent.refresh();
                    });
                });
            }
            return true;
        });

        if (Application.customTheme() != null) {
            SpannableString spanString = new SpannableString(menuItem.getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Application.customTheme().getMainColor()), 0, spanString.length(), 0);
            menuItem.setTitle(spanString);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                menuItem.setIconTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
            }
            Drawable d = menuItem.getIcon();
            if (d != null) {
                d.mutate().setColorFilter(Application.customTheme().getMainColor(), PorterDuff.Mode.SRC_IN);
                menuItem.setIcon(d);
            }
        }
    }

    private void initPanelAccount() {
        View v = findViewById(R.id.account_view);
        accountComponent = new DrawerAccountComponent(v);
        accountComponent.setOptionIcon(R.drawable.ic_account_switch_grey600_48dp);
        if (Application.sessions.size() > 1) {
            accountComponent.showSwitchAccountButton();
            accountComponent.setSwitchAccountButtonClickListener((view) -> {
                boolean showingData = dataNavigationView.getVisibility() == View.VISIBLE;
                if (showingData) {
                    accountComponent.setOptionIcon(R.drawable.ic_folder_multiple_grey600_48dp);
                    dataNavigationView.setVisibility(View.GONE);
                    sessionsNavigationView.setVisibility(View.VISIBLE);
                } else {
                    accountComponent.setOptionIcon(R.drawable.ic_account_switch_grey600_48dp);
                    sessionsNavigationView.setVisibility(View.GONE);
                    dataNavigationView.setVisibility(View.VISIBLE);
                }
            });
        } else {
            accountComponent.hideSwitchAccountButton();
        }

        accountComponent.setLogoutActionClickListener((view) -> {
            Application.clearState();
            Database.deleteToken(String.format("%s@%s", session.user, session.server.url()));
            agent.logout();
            Application.onSessionLogout(null);

            Intent intent = new Intent(Browser.this, Accounts.class);
            Browser.this.startActivity(intent);
            Browser.this.finish();
            Browser.this.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        accountComponent.setData(session);
    }

    private void initWorkspaces() {
        leftPanelDataNavigationView = findViewById(R.id.drawer_workspace_navigation_view);
        workspaceListComponent = new WorkspaceListComponent(leftPanelDataNavigationView, leftPanelWorkspaceMenu);
        workspaceListComponent.setData(this.state.workspace, session.server.workspaces, session.server.versionName().contains("cells"), this::selectWorkspace);

        if (session.server.workspaces != null) {
            listComponent.refresh();
        } else {
            getHandler().post(() -> agent.workspaces((workspaceNodes, err) -> {
                if (err != null) {
                    //showMessage("%s: %s", err.text, err.cause.getMessage());
                    //todo: handle properly error code
                    return;
                }
                ArrayList<WorkspaceNode> workspacesToPoll = new ArrayList<>(workspaceNodes);
                OfflineService.pollChanges(workspacesToPoll);
                Database.saveSession(session);
                getHandler().post(() -> workspaceListComponent.setData(this.state.workspace, session.server.workspaces, session.server.versionName().contains("cells"), this::selectWorkspace));
            }));
        }
    }

    private void initSessions() {
        if (sessionsNavigationView != null) {
            sessionListComponent = new SessionListComponent(sessionsNavigationView);
            sessionListComponent.setData(session.id(), Application.sessions);
            sessionListComponent.hide();
            sessionListComponent.setOnNewAccountButtonClicked(this::newAccount);
            sessionListComponent.setSelectionCompletion(this::switchAccount);
        }
    }

    private void initFAB() {
        fab.setImageResource(R.drawable.add);
        fab.setOnClickListener((v) -> this.openAddContentPanel());
    }

    private void initMenuItem() {
        View v = findViewById(R.id.list_item_menu);
        nodeMenuComponent = new ListItemMenuComponent(v);
        nodeMenuComponent.setContainerHeight(getContentHeight());
    }

    // Getter
    Node getCurrentNodeDir() {
        return this.listComponent.node();
    }

    String getWorkspaceSlug(Node node) {
        if (node.type() == Node.TYPE_WORKSPACE) {
            return node.id();
        } else if (node.type() == Node.TYPE_REMOTE_NODE) {
            String slug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
            if (slug != null && !"".equals(slug)) {
                return slug;
            }

            String workspaceUuid = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_UUID);
            if (workspaceUuid == null) {
                return null;
            }

            WorkspaceNode wn = session.server.findWorkspaceById(workspaceUuid);
            if (wn != null) {
                return wn.id();
            }
            return null;
        } else {
            return null;
        }
    }

    // Action bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final long id = item.getItemId();
        if (id == android.R.id.home) {
            openLeftPanel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refreshActionBar() {
        int mode = listComponent.mode();
        if (mode == BrowserPage.ModeSearch) {
            if (currentSearchPageLevel == 0) {
                this.actionBarComponent.setHomeIcon(R.drawable.search);
            } else {
                this.actionBarComponent.setHomeIcon(R.drawable.outline_close_black_48);
            }
            this.actionBarComponent.requestSearch(this.listComponent::searchRequest);
            this.actionBarComponent.turnUncoloredMode();

        } else if (mode == BrowserPage.ModePaste) {
            this.actionBarComponent.cancelSearch();
            this.actionBarComponent.turnUncoloredMode();

        } else {
            if (currentSearchPageLevel > 0) {
                this.actionBarComponent.setHomeIcon(R.drawable.outline_close_black_48);
                this.actionBarComponent.cancelSearch();
                this.actionBarComponent.turnUncoloredMode();
            } else {
                this.actionBarComponent.setHomeIcon(R.drawable.menu);
                this.actionBarComponent.cancelSearch();
                this.actionBarComponent.turnColoredMode();
            }
        }
        super.refreshActionBar();
    }

    public List<ActionData> actionBarItemList() {
        if (nodeMenuComponent.isShowing()) {
            return new ArrayList<>();
        }

        int mode = listComponent.mode();
        List<ActionData> actions = new ArrayList<>();

        if (inPasteMode) {
            if (this.selection.isForMove()) {
                ActionData close = ActionData.move(this, (v) -> moveTo());
                actions.add(close);
            } else {
                ActionData close = ActionData.copy(this, (n) -> copyTo());
                actions.add(close);
            }
            return actions;
        }

        if (inSelectionMode) {
            ActionData optionAction = ActionData.options(this, (v) -> {
                showNodeMenuPanel(this.selection);
                return true;
            });
            ActionData selectAction = ActionData.selectAll(this, !this.selection.isAllSelected(), (v) -> {
                if (selection.isAllSelected()) {
                    selection.setAllSelected(false);
                    selection.clear();
                } else {
                    selection.update(listComponent.get(n -> !NodeUtils.isRecycleBin(n)));
                    selection.setAllSelected(true);
                }

                actionBarComponent.setTitle(selection.label());
                listComponent.enterSelectionMode();
                this.refreshActionBar();
                return true;
            });
            actions.add(optionAction);
            actions.add(selectAction);
            return actions;
        }

        if (mode != BrowserPage.ModeBookmark && mode != BrowserPage.ModeSearch && currentSearchPageLevel == 0) {
            actions.add(ActionData.actionBarSearch(this, (v) -> {
                enterSearchMode();
                return true;
            }));
        }

        if (displayMode == Display.list) {
            actions.add(ActionData.actionBarGrid(this, (v) -> {
                this.displayMode = Display.grid;
                Application.setPreference(Application.PREF_DISPLAY_MODE, String.valueOf(this.displayMode));
                refreshActionBar();
                listComponent.displayModeChanged();
                return true;
            }));
        } else {
            actions.add(ActionData.actionBarList(this, (v) -> {
                this.displayMode = Display.list;
                Application.setPreference(Application.PREF_DISPLAY_MODE, String.valueOf(this.displayMode));
                refreshActionBar();
                listComponent.displayModeChanged();
                return true;
            }));
        }
        return actions;
    }

    // List integration
    void emptyContentEvent() {
        hideFAB();
        Node node = getCurrentNodeDir();
        if (node == null) {
            listComponent.setEmptyContentText(R.string.no_workspace_selected);
            listComponent.setEmptyButtonText(R.string.select_workspace);
        } else {
            listComponent.setEmptyContentText(R.string.empty_file_folder);
            listComponent.setEmptyButtonText(R.string.add_files);
        }
    }

    void contentLoadedEvent() {
        int mode = listComponent.mode();
        if (mode == BrowserPage.ModeDefault || mode == BrowserPage.ModePaste) {
            showFAB();
        }
    }

    void emptyContentAction() {
        Node node = getCurrentNodeDir();
        if (node == null) {
            openLeftPanel();
        } else {
            openAddContentPanel();
        }
    }

    public void itemClicked(Node node) {
        if (inSelectionMode) {
            if (NodeUtils.isRecycleBin(node)) {
                return;
            }

            selection.addOrRemoveNode(node);
            actionBarComponent.setTitle(selection.label());
            refreshActionBar();
            return;
        }

        boolean folder = NodeUtils.isFolder(node);
        if (folder) {
            if (this.inPasteMode) {
                this.openDirForPaste(node);

            } else {
                this.openDirPage(node);
            }
        } else {
            this.open((FileNode) node);
        }
    }

    public void itemLongClicked(Node node) {
        if (NodeUtils.isRecycleBin(node)) {
            if (!inSelectionMode) {
                showNodeMenuPanel(node);
            }
            return;
        }

        if (!inSelectionMode) {
            enterSelectionMode();
            itemClicked(node);
        }
    }

    public void itemSecondaryActionClicked(Node node) {
        if (inSelectionMode) {
            itemClicked(node);
            return;
        }
        showNodeMenuPanel(node);
    }


    // Error handling
    private void onError(Error error) {
        getHandler().post(statusView::hide);
        //showMessage("%s: %s", error.text, error.cause.getMessage());
        //todo: handle properly error code
    }


    // Menu data
    ListItemMenuData getNodeMenuData(final Node node) {

        if (node.type() == Node.TYPE_SELECTION) {
            ListItemMenuData md = new ListItemMenuData();

            md.label = node.label();
            md.resIcon = R.drawable.unknown_file;
            md.hasOption = false;
            md.actions = new ArrayList<>();

            Node currentDirNode = getCurrentNodeDir();
            if (!NodeUtils.isRecycleBin(currentDirNode)) {
                md.actions.add(ActionData.copy(this, (v) -> selectCopyTargetDir(node)));
                md.actions.add(ActionData.move(this, (v) -> selectMoveTargetDir(node)));
            }
            md.actions.add(ActionData.delete(this, (v) -> deleteSelection(node)));

            return md;

        } else {
            ListItemMenuData md = new ListItemMenuData();
            boolean isFolder = NodeUtils.isFolder(node);
            boolean isShared = NodeUtils.isShared(node);

            boolean isRecycleBin = NodeUtils.isRecycleBin(node);
            boolean isInsideRecycle = NodeUtils.isInsideRecycleBin(node);

            md.resIcon = NodeUtils.iconResource(node);
            md.label = node.label();
            md.hasOption = !isFolder;
            md.actions = new ArrayList<>();

            if (isRecycleBin) {
                md.actions.add(ActionData.clearRecycleBin(this, (v) -> clearRecycleBin(node)));
                return md;
            }

            String workspaceSlug = getWorkspaceSlug(node);
            WorkspaceNode wn = session.server.getWorkspace(workspaceSlug);

            if (isInsideRecycle) {
                md.actions.add(ActionData.restore(this, (v) -> restore(node)));
            }

            if (!wn.isActionDisabled(downloadAction) && !isFolder) {
                if (!isFolder) {
                    md.actions.add(ActionData.send(this, (v) -> send(node)));
                }
                md.actions.add(ActionData.save(this, (v) -> saveToDevice((FileNode) node)));
            }

            if (!wn.isActionDisabled(renameAction)) {
                md.actions.add(ActionData.rename(this, (v) -> rename(node)));
            }

            if (!wn.isActionDisabled(copyAction)) {
                md.actions.add(ActionData.copy(this, (v) -> selectCopyTargetDir(node)));
            }

            if (!wn.isActionDisabled(moveAction)) {
                md.actions.add(ActionData.move(this, (v) -> selectMoveTargetDir(node)));
            }

            if (!wn.isActionDisabled(shareAction)) {
                md.actions.add(ActionData.link(this, isShared, (v, checked) -> {
                    if (!isShared) {
                        generateShareLink(node);
                    } else {
                        deleteShareLink(node);
                    }
                }, (v) -> copyShareLink(node)));
            }

            if (wn.isSyncable()) {
                final int watchState = OfflineService.watchState(node.path());

                boolean isUnderWatched = watchState == OfflineService.UNDER_A_WATCHED;
                boolean isWatched = watchState == OfflineService.WATCHED;

                boolean enabled = isUnderWatched || isWatched;
                ActionData ad = ActionData.offline(this, enabled, (v, checked) -> {
                    if (checked) {
                        if (OfflineService.watchState(node.path()) == OfflineService.UNDER_A_WATCHED) {
                            return;
                        }
                        putOffline(node);
                    } else {
                        if (OfflineService.watchState(node.path()) == OfflineService.UNDER_A_WATCHED) {
                            v.setChecked(true);
                            return;
                        }
                        deleteOffline(node);
                    }
                    //listComponent.onOfflineStateChanged((FileNode) node);
                });
                md.actions.add(ad);
            }

            if (getCurrentNodeDir().type() != Node.TYPE_BOOKMARKS) {
                boolean bookmarked = Arrays.asList(new String[]{"\"true\"", "true"}).contains(node.getProperty(Pydio.NODE_PROPERTY_BOOKMARK));
                if(!bookmarked) {
                    bookmarked = NodeUtils.isBookmarked(node);
                }

                boolean finalBookmarked = bookmarked;
                ActionData bookmarkActionData = ActionData.bookmark(this, bookmarked, (v, checked) -> {
                    if (finalBookmarked) {
                        unBookmark(node);
                    } else {
                        bookmark(node);
                    }
                });
                md.actions.add(bookmarkActionData);
            }

            if (!wn.isActionDisabled(deleteAction)) {
                md.actions.add(ActionData.delete(this, (v) -> delete(node)));
            }
            return md;
        }
    }

    ListItemMenuData getImportMenuData() {
        ListItemMenuData md = new ListItemMenuData();

        WorkspaceNode ws = null;
        Node node = this.listComponent.node();
        if (node.type() == Node.TYPE_WORKSPACE) {
            ws = (WorkspaceNode) node;
        } else if (node.type() == Node.TYPE_REMOTE_NODE) {
            ws = session.resolveNodeWorkspace((FileNode) node);
        } else {
            return null;
        }


        md.label = getString(R.string.create);
        md.resIcon = R.drawable.add;
        md.hasOption = false;

        md.actions = new ArrayList<>();
        if (!ws.isActionDisabled(mkdirAction)) {
            md.actions.add(ActionData.newFolder(this, (v) -> createFolder()));
        }

        if (!ws.isActionDisabled(uploadAction) && !inPasteMode) {
            md.actions.add(ActionData.imports(this, (v) -> selectForImports()));
            md.actions.add(ActionData.camera(this, (v) -> shootAndUpload()));
        }

        return md;
    }

    // Events
    private void onPollEvents(Node nodeDir, List<Node> deleted, List<Node> updated, List<Node> added) {
        Node currentNodeDir = getCurrentNodeDir();
        if (getWorkspaceSlug(currentNodeDir).equals(getWorkspaceSlug(nodeDir)) && nodeDir.path().equals(currentNodeDir.path())) {
            try {
                synchronized (eventLock) {
                    if (deleted != null && deleted.size() > 0) {
                        listComponent.onDeleted(deleted.toArray(new Node[0]));
                    }

                    if (updated != null && updated.size() > 0) {
                        listComponent.onUpdated(updated.toArray(new Node[0]));
                    }

                    if (added != null && added.size() > 0) {
                        listComponent.onCreated(added.toArray(new Node[0]));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // State
    private void onNoWorkspaceSelected() {
        openLeftPanel();
        setActionBarTitle(getString(R.string.no_workspace_selected));
        initWorkspaces();
    }


    // Accounts
    private void newAccount() {
        Intent intent = new Intent(this, Application.newServerClass);
        startActivity(intent);
    }

    private void switchAccount(Session s) {
        if (s.id().equals(state.session)) {
            return;
        }

        ProgressDialogData data = new ProgressDialogData();
        data.iconRes = R.drawable.ic_account_switch_grey600_48dp;
        data.text = getString(R.string.loading_account);
        data.indeterminate = true;
        data.title = getString(R.string.loading);

        ProgressDialogComponent d = new ProgressDialogComponent(this, data);

        final PydioAgent newAgent = new PydioAgent(s);
        final Task t = newAgent.workspaces((nodes, error) -> {
            if (error != null) {
                showMessage(R.string.failed_to_load_account);
                d.hide();
                return;
            }

            this.state = new State();
            this.session = s;
            this.state.session = s.id();
            Application.saveState(this.state);
            this.agent = newAgent;
            getHandler().post(() -> {
                d.hide();
                initView();
            });
        });

        d.onCancelListener(t::cancel);
        d.show();
    }

    // Imports
    boolean selectForImports() {
        nodeMenuComponent.hide();
        boolean permissionGranted = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!permissionGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, IntentCode.authorizeImportFromStorage);
            } else {
                longShowMessage(R.string.manually_grant_storage_access);
            }
            return true;
        }


        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.import_files)), IntentCode.storageSelection);
        return true;
    }

    boolean createFolder() {
        //hideFAB();
        final Node node = getCurrentNodeDir();
        nodeMenuComponent.hide();
        InputDialogData data = InputDialogData.createFile(this, true, (name) ->
                agent.createDir(node, name, (msg, e) -> {
                    if (e != null) {
                        showMessage(e.toString());
                        return;
                    }

                    if (msg != null) {
                        if (msg.type().equals(Message.EMPTY)) {
                            listComponent.refresh();
                        } else {
                            onPollEvents(node, msg.deleted, msg.updated, msg.added);
                        }
                    }
                })
        );
        InputTextDialogComponent id = new InputTextDialogComponent(this, data);
        id.update(data);
        id.show();
        return true;
    }

    boolean shootAndUpload() {
        nodeMenuComponent.hide();
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File tempDir = new File(session.tempFolderPath());

            try {
                tempCameraShot = new File(tempDir, "camera_" + System.currentTimeMillis() + ".jpg");
            } catch (Exception e) {
                showMessage(R.string.unable_to_create_file_on_sd_card);
                return true;
            }

            Uri uri;
            try {
                String packageName = this.getString(R.string.file_provider_authority);
                uri = FileProvider.getUriForFile(this, packageName, tempCameraShot);
            } catch (Exception e) {
                showMessage(R.string.unable_to_share_file_with_camera);
                return true;
            }

            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                startActivityForResult(intent, IntentCode.cameraSelection);
            } catch (Exception ignore) {
            }
            return true;
        }

        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, IntentCode.authorizeImportFromCamera);
        } else {
            longShowMessage(R.string.unable_to_share_file_with_camera);
        }
        return true;
    }

    void onItemSelectedForImports(int result, Intent data) {
        if (result == RESULT_OK) {
            if (data == null) {
                showMessage(R.string.no_item_selected);
                return;
            }

            Connectivity con = Connectivity.get(this);

            if (!con.icConnected()) {
                showMessage(R.string.no_active_connection);
                return;
            }

            Runnable action = () -> {
                List<String> files = new ArrayList<>();

                ClipData clip;
                if ((clip = data.getClipData()) != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        ClipData.Item item = clip.getItemAt(i);
                        Uri uri = item.getUri();
                        String path = ContentResolver.getPath(this, uri);
                        if (path != null) {
                            files.add(path);
                        }
                    }

                } else {
                    Uri uri = data.getData();
                    if (uri == null) {
                        showMessage(R.string.no_item_selected);
                        return;
                    }

                    String path = ContentResolver.getPath(this, uri);
                    if (path == null) {
                        showMessage(R.string.failed_to_get_file_content);
                        return;
                    }
                    files.add(path);
                }

                if (files.size() > 0) {
                    upload(files);
                }
            };

            if (con.isCellular() && !con.isCellularDownloadAllowed()) {
                DialogData dialogData = DialogData.confirmUploadOnCellularData(this, action);
                new ConfirmDialogComponent(this, dialogData).show();
            } else {
                action.run();
            }
        }
    }

    void onImageShotForUpload(int result, Intent data) {
        if (result == RESULT_OK) {
            List<String> files = new ArrayList<>();
            final File file = tempCameraShot;
            tempCameraShot = null;
            files.add(file.getPath());
            upload(files);
        }
    }

    // Menu Panels
    void showNodeMenuPanel(Node node) {
        ListItemMenuData md = getNodeMenuData(node);
        nodeMenuComponent.setData(md);
        nodeMenuComponent.onShow = () -> {
            this.hideFAB();
            this.refreshActionBar();
            this.actionBarComponent.hideHomeIcon();
            this.actionBarComponent.setTitle(node.label());
        };
        nodeMenuComponent.onHide = () -> {
            Node currentNodeDir = this.getCurrentNodeDir();
            this.refreshActionBar();
            this.actionBarComponent.shoHomeIcon();
            this.actionBarComponent.setTitle(currentNodeDir.label());
            if (inSelectionMode) {
                return;
            }
            if (listComponent.count() > 0) {
                this.showFAB();
            } else {
                this.hideFAB();
            }
        };
        nodeMenuComponent.show();
    }

    void openAddContentPanel() {
        ListItemMenuData md = getImportMenuData();
        if (md != null && md.actions != null && md.actions.size() > 0) {
            nodeMenuComponent.setData(md);
            nodeMenuComponent.onShow = this::hideFAB;
            nodeMenuComponent.onHide = () -> {
                if (listComponent.count() > 0) {
                    this.showFAB();
                } else {
                    this.hideFAB();
                }
            };
            nodeMenuComponent.show();
        } else {
            showMessage(R.string.upload_not_authorized);
        }
    }

    // Actions
    void requestWorkspaceNodeList() {
        this.agent.workspaces((workspaces, error) -> {
            if (error != null) {
                this.showMessage(error.toString());
                if (error.code == Code.authentication_required) {
                    getHandler().post(() -> {
                        this.onAuthenticationRequired(this::requestWorkspaceNodeList);
                    });
                }
                return;
            }
            Database.saveSession(session);
            getHandler().post(this::onNoWorkspaceSelected);
        });
    }

    private void selectWorkspace(final WorkspaceNode wn) {
        nodeMenuComponent.hide();
        closeLeftPanel();
        state.workspace = wn.getId();
        workspaceSelected = false;

        listComponent.clear();

        if (!wn.isLoaded()) {
            taskDialog.show();
            TaskDialogData data = new TaskDialogData();
            data.icon = R.drawable.folder;
            data.name = getString(R.string.loading_workspace);
            data.progress = 0;
            data.title = wn.label();
            taskDialog.update(data);

            agent.workspaceInfo(wn.id(), (n, error) -> {
                Application.saveSession(session);
                ignoreACLs = error != null;
                Application.saveSession(session);
                state.save();
                taskDialog.hide();
                if (thumbStore != null) {
                    thumbStore.stop();
                }
                thumbStore = new ThumbStore(this.session);
                thumbStore.start();
                getHandler().post(() -> this.openDirPage(wn));
            });
        } else {
            state.save();
            if (thumbStore != null) {
                thumbStore.stop();
            }
            thumbStore = new ThumbStore(this.session);
            thumbStore.start();
            getHandler().post(() -> this.openDirPage(wn));
            agent.workspaceInfo(wn.id(), (n, error) -> Application.saveSession(session));
        }
    }

    boolean saveToDevice(FileNode node) {
        String workspaceSlug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
        nodeMenuComponent.hide();
        String localFilePath;
        localFilePath = session.publicDownloadPath(node.path());

        File localFile = new File(localFilePath);
        if (!Connectivity.get(this).icConnected() && !localFile.exists()) {
            showMessage(R.string.no_active_connection);
            return true;
        }

        Background.go(() -> {
            String localFileHash = LocalFS.md5(localFilePath);
            Stats stats = null;
            try {
                stats = agent.client.stats(workspaceSlug, node.path(), true);
            } catch (SDKException e) {
                e.printStackTrace();
            }

            if (stats != null && !localFileHash.equals(stats.getHash())) {
                Connectivity con = Connectivity.get(this);
                if (con.isCellular() && !con.isCellularDownloadAllowed()) {
                    Stats finalStats = stats;
                    DialogData data = DialogData.confirmDownloadOnCellularData(this, node.label(), node.size(), () -> downloadFile(node, finalStats));
                    new ConfirmDialogComponent(this, data).show();
                } else {
                    downloadFile(node, stats);
                }
            }
        });
        return true;
    }

    boolean putOffline(final Node node) {
        nodeMenuComponent.hide();
        OfflineService.watchPath(node.path());
        getHandler().postDelayed(() -> this.listComponent.onWatched(node), 1000);
        return true;
    }

    boolean deleteOffline(Node node) {
        nodeMenuComponent.hide();
        if (this.session.server.versionName().contains("cells")) {
            return true;
        }

        String workspaceSlug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
        OfflineService.unWatchPath(node.path());
        if (this.hasOfflineVersion((FileNode) node)) {
            String offlinePath = session.downloadPath(workspaceSlug, node.path());
            if (Application.localSystem.delete(offlinePath)) {
                showMessage(R.string.offline_deleted);
            }
        }
        getHandler().postDelayed(() -> this.listComponent.onUnWatched(node), 1000);
        return true;
    }

    boolean rename(Node node) {
        nodeMenuComponent.hide();
        final Node parentNode = getCurrentNodeDir();
        InputDialogData data = InputDialogData.rename(this, node.label(), true,
                name -> {
                    // send event in advance to match it up with poll events
                    if (this.session.server.versionName().contains("cells")) {
                        listComponent.onRenamed(node, name);
                    }

                    agent.rename(node, name, (msg, e) -> {
                        if (e != null) {
                            showMessage(R.string.failed_to_rename_file, node.label(), name);
                            return;
                        }

                        showMessage(R.string.file_renamed, node.label(), name);
                        if (msg != null && msg.hasEvents()) {
                            onPollEvents(parentNode, msg.deleted, msg.updated, msg.added);
                        } else {
                            listComponent.onRenamed(node, name);
                        }
                    });
                }
        );
        InputTextDialogComponent id = new InputTextDialogComponent(this, data);
        id.update(data);
        id.show();
        return true;
    }

    boolean generateShareLink(Node node) {
        nodeMenuComponent.hide();
        final Node parentNode = getCurrentNodeDir();
        String workspaceSlug = getWorkspaceSlug(node);
        WorkspaceNode workspaceNode = session.server.getWorkspace(workspaceSlug);
        Plugin share = workspaceNode.getPlugin("action.share");

        Completion<String> generate = (password) -> agent.generateShareLink(node, password, ((FileNode) node).isFolder(), (link, error) -> {
            if (error != null) {
                showMessage(error.toString());
                return;
            }

            node.setProperty(Pydio.NODE_PROPERTY_SHARE_LINK, link);
            node.setProperty(Pydio.NODE_PROPERTY_AJXP_SHARED, "true");
            String cacheWs = session.workspaceCacheID(workspaceSlug);
            Cache.update(cacheWs, getCurrentNodeDir().path(), node.label(), node);
            listComponent.onShared(node);

            getHandler().post(() -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(node.label(), link);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    showMessage(R.string.link_copied_to_clip);
                } else {
                    showMessage(R.string.link_copy_failed);
                }
            });
        });
        if (share == null) {
            generate.onComplete("");
            return true;
        }

        String forcePassword = share.configs.getProperty("SHARE_FORCE_PASSWORD");
        if ("true".equals(forcePassword)) {
            InputDialogData dialogData = InputDialogData.password(this, getString(R.string.link_password), getString(R.string.generate_link), generate);
            InputTextDialogComponent inputDialog = new InputTextDialogComponent(this, dialogData);
            inputDialog.show();
        } else {
            generate.onComplete("");
        }
        return true;
    }

    boolean deleteShareLink(Node node) {
        nodeMenuComponent.hide();
        Node currentNode = getCurrentNodeDir();
        agent.deleteShareLink(node, (error) -> {
            if (error != null) {
                showMessage(R.string.failed_to_delete_share_link);
                return;
            }

            node.deleteProperty(Pydio.NODE_PROPERTY_SHARE_UUID);
            node.deleteProperty(Pydio.NODE_PROPERTY_SHARE_LINK);
            node.deleteProperty(Pydio.NODE_PROPERTY_AJXP_SHARED);
            Cache.update(state.workspace, currentNode.path(), node.label(), node);
            listComponent.onUnShared(node);
        });
        return true;
    }

    boolean copyShareLink(Node node) {
        String workspaceSlug = getWorkspaceSlug(node);
        nodeMenuComponent.hide();
        StringCompletion completion = (link) -> {
            Browser.this.getHandler().post(() -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(node.label(), link);

                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    showMessage(R.string.link_copied_to_clip);
                } else {
                    showMessage(R.string.link_copy_failed);
                }
            });
        };

        String link = node.getProperty(Pydio.NODE_PROPERTY_SHARE_LINK);
        if (link != null) {
            completion.onComplete(link);
            return true;
        }

        String shareUUID = node.getProperty(Pydio.NODE_PROPERTY_SHARE_UUID);
        if (shareUUID == null) {
            shareUUID = node.path();
        }

        String finalShareUUID = shareUUID;
        agent.shareInfo(node, (json, error) -> {
            if (error != null) {
                showMessage(Browser.this.getString(R.string.failed_to_load_share_link), node.label());
                return;
            }
            String loadedLink = null;
            try {
                if (json.has("LinkUrl")) {
                    loadedLink = json.getString("LinkUrl");
                } else {
                    JSONObject links = json.getJSONObject("links");
                    Iterator it = links.keys();
                    while (it.hasNext()) {
                        String key = (String) it.next();
                        JSONObject linkDetails = links.getJSONObject(key);
                        loadedLink = linkDetails.getString("public_link");
                    }
                }
            } catch (Exception e) {
                showMessage(Browser.this.getString(R.string.failed_to_load_share_link), node.label());
                return;
            }

            if (loadedLink == null) {
                showMessage(Browser.this.getString(R.string.failed_to_load_share_link), node.label());
                return;
            }

            try {
                node.setProperty(Pydio.NODE_PROPERTY_SHARE_LINK, loadedLink);
                Cache.update(workspaceSlug, this.getCurrentNodeDir().path(), node.label(), node);
            } catch (Exception ignore) {
            }
            completion.onComplete(loadedLink);
        });
        return true;
    }

    boolean send(Node node) {
        String workspaceSlug = getWorkspaceSlug(node);
        nodeMenuComponent.hide();
        File file = new File(session.downloadPath(workspaceSlug, node.path()));
        if (file.exists()) {
            sendFile(file);
            return true;
        }

        Runnable action = () -> {
            TaskDialogData d = new TaskDialogData();
            d.indeterminate = false;
            d.title = getString(R.string.download_title);
            d.icon = NodeUtils.iconResource(node);
            d.name = node.label();
            d.withActions = false;
            taskDialog.show();
            taskDialog.update(d);

            Background.go(() -> {
                Transfer t = Transfer.newDownload(session, workspaceSlug, (FileNode) node);
                t.run((progress) -> {
                    d.progress = (int) ((progress * 100) / ((FileNode) node).size());
                    getHandler().post(() -> taskDialog.update(d));
                }, (msg, error) -> {
                    taskDialog.hide();
                    if (error != null) {
                        showMessage(R.string.failed_to_download_file, node.label());
                        return;
                    }
                    sendFile(new File(session.downloadPath(workspaceSlug, node.path())));
                });
            });
        };

        Connectivity con = Connectivity.get(this);
        if (!con.icConnected()) {
            showMessage(R.string.no_active_connection);
            return true;
        }

        if (con.isCellular() && !con.isCellularDownloadAllowed()) {
            DialogData data = DialogData.confirmDownloadOnCellularData(this, node.label(), node.getProperty(Pydio.NODE_PROPERTY_BYTESIZE), action);
            new ConfirmDialogComponent(this, data).show();
        } else {
            action.run();
        }
        return true;
    }

    boolean deleteSelection(Node node) {
        nodeMenuComponent.hide();
        final List<Node> selectedNodes = this.selection.nodes();
        exitSelectionMode();
        DialogData data = new DialogData();
        data.action = () -> {
            Node[] nodes = new Node[selectedNodes.size()];
            for (int i = 0; i < selectedNodes.size(); i++) {
                nodes[i] = selectedNodes.get(i);
            }
            final Message[] message = {null};
            this.agent.delete(nodes, (msg, error) -> {
                if (error != null) {
                    this.showMessage(error.toString());
                    return;
                }

                if (message[0] == null) {
                    message[0] = msg;
                } else {
                    message[0].deleted.addAll(msg.deleted);
                    message[0].added.addAll(msg.added);
                    message[0].updated.addAll(msg.updated);
                }
            });

            Message msg = message[0];
            this.showMessage(R.string.selected_files_deleted);
            if (msg != null && msg.hasEvents()) {
                onPollEvents(selection.in(), msg.deleted, msg.updated, msg.added);
            } else {
                for (Node n: selectedNodes) {
                    listComponent.onDeleted(n);
                }
            }
        };
        data.iconRes = R.drawable.delete;
        data.message = getString(R.string.delete_selection);
        data.positiveText = getString(R.string.delete);
        data.title = getString(R.string.delete);

        ConfirmDialogComponent d = new ConfirmDialogComponent(this, data);
        d.show();
        return true;
    }

    boolean delete(Node node) {
        if (this.selection == null) {
            this.selection = new Selection(this, this.getCurrentNodeDir());
        } else {
            selection.clear();
        }

        nodeMenuComponent.hide();

        FileNode n = (FileNode) node;
        DialogData data = DialogData.removeFile(this, node.label(), n.isFolder(), false, () ->
                agent.delete(new Node[]{node}, (msg, error) -> {
                    if (error != null) {
                        showMessage(R.string.failed_to_delete_file, node.label());
                        return;
                    }

                    this.showMessage(getString(R.string.file_deleted), node.label());
                    if (!session.server.versionName().contains("cells") && msg != null && msg.hasEvents()) {
                        onPollEvents(selection.in(), msg.deleted, msg.updated, msg.added);
                    } else {
                        this.listComponent.onDeleted(node);
                    }
                    deleteOffline(node);
                }));
        ConfirmDialogComponent cd = new ConfirmDialogComponent(this, data);
        cd.show();
        return true;
    }

    boolean selectCopyTargetDir(Node node) {
        nodeMenuComponent.hide();
        if (node.type() != Node.TYPE_SELECTION) {
            if (this.selection == null) {
                this.selection = new Selection(this, this.getCurrentNodeDir());
            }
            this.selection.addOrRemoveNode(node);
        } else {
            exitSelectionMode();
        }
        this.selection.setForMove(false);
        getHandler().postDelayed(this::enterPasteMode, 1000);
        return true;
    }

    boolean selectMoveTargetDir(Node node) {
        nodeMenuComponent.hide();
        if (node.type() != Node.TYPE_SELECTION) {
            if (this.selection == null) {
                this.selection = new Selection(this, this.getCurrentNodeDir());
            }
            selection.addOrRemoveNode(node);
        } else {
            exitSelectionMode();
        }
        this.selection.setForMove(true);
        this.enterPasteMode();
        return true;
    }

    boolean copyTo() {
        final Node to = getCurrentNodeDir();
        final List<Node> selectedNodes = this.selection.nodes();
        exitPasteMode();
        agent.copy(selectedNodes, to, (msg, error) -> {
            if (error != null) {
                showMessage(R.string.failed_to_copy_files);
                return;
            }
            showMessage(String.format(getString(R.string.files_copied_to), to.label()));
        });
        return true;
    }

    boolean moveTo() {
        final Node to = getCurrentNodeDir();
        final List<Node> selectedNodes = this.selection.nodes();
        exitPasteMode();

        agent.move(selectedNodes, to, (msg, error) -> {
            if (error != null) {
                showMessage(R.string.failed_to_move_files);
                return;
            }

            if (msg != null && msg.hasEvents()) {
                onPollEvents(selection.in(), msg.deleted, msg.updated, msg.added);
            } else {
                onPollEvents(selection.in(), selectedNodes, null, null);
            }
            showMessage(String.format(getString(R.string.files_moved_to), to.label()));
        });
        return true;
    }

    void upload(final List<String> filenames) {

        Node node = getCurrentNodeDir();
        String workspaceSlug = getWorkspaceSlug(node);
        final String dir = node.path();

        final Handler h = getHandler();

        final TaskDialogData data = new TaskDialogData();
        data.progress = 0;
        data.name = getString(R.string.preparing);
        data.icon = R.drawable.upload;
        data.title = data.name;
        taskDialog.update(data);

        thumbStore.stop();
        Background.go(() -> {
            h.post(() -> taskDialog.show());

            for (String filename : filenames) {
                File file = new File(filename);
                final String name = file.getName();

                final TaskDialogData d = new TaskDialogData();
                d.name = d.title = name;
                d.progress = 0;
                d.icon = NodeUtils.iconResource(name);
                d.withActions = false;
                d.indeterminate = true;

                try {
                    Message msg = agent.client.upload(file, workspaceSlug, dir, name, true,
                            (p) -> {
                                d.indeterminate = false;
                                d.progress = (int) ((p * 100) / file.length());
                                h.post(() -> taskDialog.update(d));
                                return false;
                            }
                    );

                    thumbStore.start();
                    if (msg != null && msg.hasEvents()) {
                        for (Node added : msg.added) {
                            added.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug);
                        }
                        for (Node updated : msg.updated) {
                            updated.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug);
                        }
                        onPollEvents(node, msg.deleted, msg.updated, msg.added);
                    }
                } catch (SDKException e) {
                    e.printStackTrace();
                    showMessage(R.string.failed_to_upload_file, name);
                }
            }
            h.post(() -> taskDialog.hide());
        });
    }

    boolean clearRecycleBin(Node node) {
        String workspaceSlug = getWorkspaceSlug(node);

        nodeMenuComponent.hide();
        agent.emptyRecycleBin(workspaceSlug, (msg, error) -> {
            if (error != null) {
                showMessage(R.string.failed_to_empty_recycle_bin);
                return;
            }
            if (!agent.client.getServerNode().versionName().contains("cells")) {
                showMessage(R.string.recycle_cleared);
                listComponent.refresh();
            }
        });
        return true;
    }

    boolean restore(Node node) {
        nodeMenuComponent.hide();
        agent.restore(node, (msg, error) -> {
            if (error != null) {
                showMessage(R.string.failed_to_restore_the_file);
                return;
            }

            if (!agent.client.getServerNode().versionName().contains("cells")) {
                showMessage(R.string.file_restored);
                listComponent.refresh();
            }
        });
        return true;
    }

    void open(FileNode node) {
        boolean isImage = NodeUtils.isImage(node);

        if (isImage) {
            openImage(node);
            return;
        }

        String workspaceSlug = getWorkspaceSlug(node);
        final String localFilePath = session.downloadPath(workspaceSlug, node.path());
        File localFile = new File(localFilePath);


        Connectivity con = Connectivity.get(this);
        if (!con.icConnected() && !localFile.exists()) {
            showMessage(R.string.no_active_connection);
            return;
        }

        Background.go(() -> {
            Stats stats;
            try {
                stats = agent.client.stats(workspaceSlug, node.path(), true);
            } catch (SDKException e) {
                e.printStackTrace();
                getHandler().post(() -> showMessage(R.string.failed_to_get_file_content));
                return;
            }

            String localFileHash = LocalFS.md5(localFilePath);

            if (stats != null && !localFileHash.equals(stats.getHash())) {
                promptToDownloadOnCellular(node);
            } else {
                getHandler().post(() -> openExternal(localFile));
            }
        });
    }

    void openImage(Node node) {
        Background.go(() -> {
            try {
                String workspaceSlug = getWorkspaceSlug(node);
                Stats stats = agent.client.stats(workspaceSlug, node.path(), false);
                if (stats == null) {
                    getHandler().post(() -> showMessage(R.string.failed_to_get_file_content));
                    return;
                }

                // String workspaceSlug = getWorkspaceSlug(node);
                int index = 0;
                List<Node> images = listComponent.get(NodeUtils::isImage);
                for (Node n : images) {
                    if (n.label().equals(node.label())) {
                        break;
                    }
                    index++;
                }

                PreviewerData data = new PreviewerData();
                data.setIndex(index);
                data.setNodes(images);
                data.setSession(session);
                Application.previewerActivityData = data;

                getHandler().post(() -> {
                    Intent intent = new Intent(this, MediaViewer.class);
                    startActivityForResult(intent, IntentCode.open);
                });

            } catch (SDKException e) {
                e.printStackTrace();
                getHandler().post(() -> showMessage(R.string.failed_to_get_file_content));
            }
        });
    }

    void promptToDownloadOnCellular(FileNode node) {
        Connectivity con = Connectivity.get(this);
        if (con.isCellular() && !con.isCellularDownloadAllowed()) {
            getHandler().post(() -> {
                DialogData data = DialogData.confirmDownloadOnCellularData(this, node.label(), node.size(), () -> downloadAndOpen(node));
                new ConfirmDialogComponent(this, data).show();
            });
        } else {
            downloadAndOpen(node);
        }
    }

    void downloadAndOpen(FileNode node) {
        String workspaceSlug = getWorkspaceSlug(node);

        final String localFilePath = session.downloadPath(workspaceSlug, node.path());
        File file = new File(localFilePath);
        Background.go(() -> agent.download(node, file, (progress) -> {
                    TaskDialogData data = new TaskDialogData();
                    data.progress = (int) ((progress * 100) / node.size());
                    data.icon = R.drawable.download;
                    data.name = node.label();
                    data.title = Browser.this.getString(R.string.download);
                    getHandler().post(() -> taskDialog.update(data));
                    return false;
                }, (error) -> {
                    if (error != null) {
                        showMessage(R.string.failed_to_get_file_content);
                        return;
                    }
                    getHandler().post(() -> taskDialog.hide());
                    openExternal(file);
                }
        ));
    }

    void downloadFile(FileNode node, final Stats stats) {
        String localFilePath;
        localFilePath = session.publicDownloadPath(node.label());
        TaskDialogData data = new TaskDialogData();
        data.icon = R.drawable.download;
        data.name = node.label();
        data.title = Browser.this.getString(R.string.download_title);
        data.indeterminate = false;

        String workspaceSlug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
        File file = new File(localFilePath);
        getHandler().post(() -> agent.download(node, file, (progress) -> {
                    data.progress = (int) (progress * 100 / node.size());
                    getHandler().post(() -> taskDialog.update(data));
                    return false;
                }, (error) -> {
                    getHandler().post(() -> taskDialog.hide());

                    if (LocalFS.md5(localFilePath).equals(stats.getHash())) {
                        showMessage(R.string.file_downloaded, node.label());
                        return;
                    }

                    if (error != null) {
                        showMessage(R.string.failed_to_download_file, node.label());
                    }
                }
        ));
    }

    void bookmark(Node node) {
        nodeMenuComponent.hide();
        agent.bookmark(node, (msg, error) -> {
            if (error != null) {
                showMessage(error.toString());
                return;
            }
            showMessage(getString(R.string.file_bookmarked), node.label());
            node.setProperty(Pydio.NODE_PROPERTY_BOOKMARK, "true");
            node.setProperty(Pydio.NODE_PROPERTY_AJXP_BOOKMARKED, "true");
            listComponent.onBookmarked(node);
        });
    }

    void unBookmark(Node node) {
        nodeMenuComponent.hide();
        agent.unbookmark(node, (msg, error) -> {
            if (error != null) {
                showMessage(error.toString());
                return;
            }
            showMessage(getString(R.string.file_unbookmarked), node.label());
            node.deleteProperty(Pydio.NODE_PROPERTY_BOOKMARK);
            node.deleteProperty(Pydio.NODE_PROPERTY_AJXP_BOOKMARKED);
            listComponent.onUnBookmarked(node);
        });
    }

    // Page
    private void openDirPage(Node node) {
        if (inSearchMode) {
            currentSearchPageLevel++;
        }

        if (thumbStore == null) {
            thumbStore = new ThumbStore(this.session);
        }
        thumbStore.start();
        if (node.type() == Node.TYPE_WORKSPACE) {
            WorkspaceNode wn = (WorkspaceNode) node;
            this.workspaceSelected = true;
            OfflineService.startWorkspace(wn.slug());

        } else if (node.type() == Node.TYPE_REMOTE_NODE) {
            String slug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
            if (slug == null) {
                slug = getWorkspaceSlug(node);
                if (slug != null) {
                    node.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, slug);
                }
            }
        }
        ContentPageState data = new ContentPageState();
        data.mode = BrowserPage.ModeDefault;
        data.guiContext = this;
        data.activityContext = this;
        data.node = node;
        data.displayInfo = this;
        data.metrics = metrics;
        data.session = session;
        data.sorter = this.sorters.get(sorter);
        listComponent.pushNew(data);
    }

    private void openDirForPaste(Node node) {
        if (thumbStore == null) {
            thumbStore = new ThumbStore(this.session);
        }
        thumbStore.start();
        pasteBrowsingLevel++;
        ContentPageState data = new ContentPageState();
        data.mode = BrowserPage.ModePaste;
        data.guiContext = this;
        data.activityContext = this;
        data.node = node;
        data.displayInfo = this;
        data.metrics = metrics;
        data.session = session;
        data.sorter = this.sorters.get(sorter);
        listComponent.pushNew(data);
    }

    private void openSearchPage() {
        inSearchMode = true;
        this.currentSearchPageLevel = 0;
        if (thumbStore == null) {
            thumbStore = new ThumbStore(this.session);
        }
        thumbStore.start();
        ContentPageState data = new ContentPageState();
        data.mode = BrowserPage.ModeSearch;
        data.guiContext = this;
        data.activityContext = this;
        data.node = getCurrentNodeDir();
        data.displayInfo = this;
        data.metrics = metrics;
        data.session = session;
        data.sorter = this.sorters.get(sorter);
        listComponent.pushNew(data);
    }

    private void browseBookmarks() {
        if (thumbStore == null) {
            thumbStore = new ThumbStore(this.session);
        }
        thumbStore.start();

        BookmarkNode node = new BookmarkNode(getString(R.string.bookmarks));
        listComponent.clear();
        ContentPageState data = new ContentPageState();
        data.guiContext = this;
        data.activityContext = this;
        data.mode= BrowserPage.ModeBookmark;
        data.displayInfo = this;
        data.metrics = metrics;
        data.session = session;
        data.sorter = this.sorters.get(sorter);
        data.node = node;
        listComponent.pushNew(data);
    }

    // Files action and share
    private boolean manageable(Node node, File file, String action) {
        if (node.label().endsWith(".apk")) {
            return true;
        }
        try {
            Intent intent = new Intent();
            intent.setAction(action);
            String packageName = getString(R.string.file_provider_authority);
            Uri uri = FileProvider.getUriForFile(this, packageName, file);
            String mime = FileUtils.mimeType(node.label());
            intent.setDataAndType(uri, mime);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PackageManager manager = getPackageManager();
            List<ResolveInfo> list = manager.queryIntentActivities(intent, 0);
            return list != null && list.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendFile(File file) {
        String mime = FileUtils.getMimeType(file.getPath());
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String packageName = getString(R.string.file_provider_authority);
        Uri uri = FileProvider.getUriForFile(this, packageName, file);
        sendIntent.setType(mime);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(sendIntent);
        } catch (Exception e) {
            showMessage(getString(R.string.no_installed_app_for_send_file), file.getName());
        }
    }

    private void openExternal(File file) {
        String mime = FileUtils.getMimeType(file.getName());
        try {
            Intent intent = new Intent();
            String authority = getString(R.string.file_provider_authority);
            Uri uri = FileProvider.getUriForFile(this, authority, file);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, IntentCode.open);
        } catch (Exception e) {
            showMessage(R.string.no_installed_app_for_open);
        }
    }

    // Activity context
    @Override
    public void setTitle(String title) {
        if (inPasteMode) {
            if (this.selection.isForMove()) {
                title = String.format(getString(R.string.move_to), title);
            } else {
                title = String.format(getString(R.string.copy_to), title);
            }
        }
        setActionBarTitle(title);
        refreshActionBar();
    }
    @Override
    public void setTitle(int resTitle) {
        setActionBarTitle(getString(resTitle));
        refreshActionBar();
    }
    @Override
    public ThumbLoader thumbLoader() {
        if (thumbStore != null) {
            return thumbStore.getDelegate();
        }
        return null;
    }
    @Override
    public int mode() {
        return displayMode;
    }
    @Override
    public void onAuthenticationRequired(Runnable afterAuth) {
        if (this.session.server.supportsOauth()) {
            this.onOAuthCallback = afterAuth;
            OauthConfig cfg = OauthConfig.fromJSON(this.session.server.getOIDCInfo(), "");
            com.pydio.android.client.accounts.Accounts.manager.authorize(cfg, this);
            return;
        }

        Handler h = new Handler(this.getMainLooper());
        h.post(() -> {
            LoginDialogComponent ld = new LoginDialogComponent(this, this.agent, afterAuth);
            ld.show();
        });
    }

    public OfflineInfo getOfflineInfo() {
        return this;
    }

    public SelectionInfo getSelectionInfo() {
        return this;
    }

    @Override
    public void sessionUpdated(Session newSession) {
        if (!this.session.equals(newSession)) {
            this.session = newSession;
            this.agent.session = newSession;
        }

        if (this.listComponent != null) {
            this.listComponent.refresh();
        }
    }

    // Offline
    @Override
    public boolean isWatched(FileNode node) {
        return OfflineService.watchState(node.path()) == OfflineService.WATCHED;
    }
    @Override
    public boolean isUnderAWatchedFolder(FileNode node) {
        return OfflineService.watchState(node.path()) == OfflineService.UNDER_A_WATCHED;
    }
    @Override
    public boolean hasOfflineVersion(FileNode node) {
        String workspace = getWorkspaceSlug(node);
        File file = new File(session.downloadPath(workspace, node.path()));
        return file.exists();
    }
    @Override
    public String status(FileNode node) {
        return null;
    }
    @Override
    public void setEventListener(EventsListener listener) {
        //offlineEventListener = listener;
    }
    @Override
    public void onNewChanges(String session, String workspace, int count) {
        if (session.equals(this.session.id())) {
            getHandler().post(() -> this.workspaceListComponent.notifyChangesCount(workspace, count));
        }
    }
    @Override
    public void onProcessingChange(String session, String workspace, String path) {}
    @Override
    public void onChangeProcessed(String session, String workspace, String path) {}
    @Override
    public void onChangeFailed(String session, String workspace, String path) {}

    // Selection Info
    @Override
    public boolean inSelectionMode() {
        return inSelectionMode;
    }
    @Override
    public boolean isSelected(Node node) {
        if (this.inSelectionMode || inPasteMode) {
            return this.selection.index(node) > -1;
        }
        return false;
    }
    @Override
    public boolean allSelected() {
        return this.selection.isAllSelected();
    }

    // Display mode
    private void enterSelectionMode() {
        final Node inDir = getCurrentNodeDir();
        inSelectionMode = true;
        selection = new Selection(this, inDir);
        listComponent.enterSelectionMode();
        actionBarComponent.setHomeIcon(R.drawable.outline_close_black_48);
        refreshActionBar();
        actionBarComponent.setTitle(selection.label());
        hideFAB();
    }

    private void exitSelectionMode() {
        actionBarComponent.setHomeIcon(R.drawable.menu);
        inSelectionMode = false;
        listComponent.exitSelectionMode();
        refreshActionBar();
        showFAB();
    }

    public void enterPasteMode() {
        nodeMenuComponent.hide();
        this.inSelectionMode = false;
        this.inPasteMode = true;
        // at this point we know for sure that we are not coming from bookmarks or search result
        Node node = this.selection.nodes().get(0);
        String ws = getWorkspaceSlug(node);
        WorkspaceNode wn = this.session.server.getWorkspace(ws);
        this.pasteBrowsingLevel = 0;
        this.setActionBarHomeIcon(R.drawable.outline_close_black_48);
        this.openDirForPaste(wn);
    }

    public void enterSearchMode() {
        this.inSearchMode = true;
        this.currentSearchPageLevel = 0;
        this.setActionBarHomeIcon(R.drawable.search);
        this.openSearchPage();
    }

    public void exitPasteMode() {
        actionBarComponent.setHomeIcon(R.drawable.menu);
        this.inPasteMode = false;
        for (int i = 0; i < pasteBrowsingLevel; i++) {
            onBackPressed();
        }
        pasteBrowsingLevel = 0;
    }

    public void exitSearchMode() {
        for (int i = 0; i <= currentSearchPageLevel; i++) {
            onBackPressed();
        }
        actionBarComponent.setHomeIcon(R.drawable.menu);
        currentSearchPageLevel = 0;
    }

    private void promptToExitPasteMode() {
        DialogData data = new DialogData();
        data.action = this::exitPasteMode;
        data.iconRes = R.drawable.logout;
        data.message = getString(R.string.exit_paste_message);
        data.positiveText = getString(R.string.skip_paste);
        data.title = getString(R.string.cancel);

        ConfirmDialogComponent d = new ConfirmDialogComponent(this, data);
        d.show();
    }

    // Transfers callback
    @Override
    public void onNew(String session, String ws, String dir, String name, int type, long size) {

    }

    @Override
    public void onProgress(String session, String ws, String dir, String name, int type, long progress, long size) {

    }

    @Override
    public void onError(String session, String ws, String dir, String name, int type, Error error) {

    }

    @Override
    public void onFinish(String session, String ws, String dir, String name, int type, Message msg) {

    }

    @Override
    public void onError(String error, String description) {
        showMessage(error + ": " + description);
    }

    @Override
    public void handleToken(String jwt) throws IOException {
        Token t;
        try {
            t = Token.decodeOauthJWT(jwt);
        } catch (ParseException e) {
            e.printStackTrace();
            showMessage(this.getString(R.string.could_not_get_token));
            return;
        }

        JWT accessToken = JWT.parse(t.idToken);
        if (accessToken == null) {
            showMessage(this.getString(R.string.could_not_decode_id_token));
            return;
        }

        t.expiry = System.currentTimeMillis()/1000 + t.expiry;
        String url = this.agent.session.server.url();

        t.subject = String.format("%s@%s", accessToken.claims.name,  url);
        Database.saveToken(t);

        Session session = new Session();
        session.user = accessToken.claims.name;
        session.server = this.agent.session.server;
        this.sessionUpdated(session);
    }

    @Override
    public void startIntent(Intent intent) {
        this.startActivity(intent);
    }
}

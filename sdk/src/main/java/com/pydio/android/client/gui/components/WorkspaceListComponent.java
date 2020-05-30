package com.pydio.android.client.gui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.callback.Completion;
import com.pydio.android.client.data.callback.IntegerCompletion;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.model.WorkspaceNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkspaceListComponent extends Component {

    private View rootView;
    private Context context;
    private String current;
    private List<String> sortedKeys;
    private Map<String, WorkspaceNode> data;
    private Completion<WorkspaceNode> completion;
    private Map<String, IntegerCompletion> changeCountHandlers;
    private boolean serverCells;
    private Menu menu;

    public WorkspaceListComponent(View rootView, Menu menu) {
        this.menu = menu;
        this.rootView = rootView;
        this.context = rootView.getContext();
        this.changeCountHandlers = new HashMap<>();
        initView();
    }

    public void setData(String current, Map<String, WorkspaceNode> data, boolean serverCells, Completion<WorkspaceNode> c) {
        this.current = current;
        sortedKeys = new ArrayList<>();
        this.data = data;
        for (Map.Entry e : data.entrySet()) {
            sortedKeys.add(e.getKey().toString());
        }
        Collections.sort(sortedKeys);

        this.serverCells = serverCells;
        this.completion = c;
        populate();
    }

    public void notifyChangesCount(String workspaceId, int count) {
        if (changeCountHandlers.containsKey(workspaceId)) {
            IntegerCompletion completion = changeCountHandlers.get(workspaceId);
            if (completion != null) {
                completion.onComplete(count);
            }
        }
    }

    public void setCurrentWorkspace(String ws) {
        this.current = ws;
        populate();
    }

    @SuppressLint("DefaultLocale")
    private void populate() {

        SubMenu myWorkspaceMenuItem = menu.findItem(R.id.my_workspaces).getSubMenu();
        myWorkspaceMenuItem.clear();
        myWorkspaceMenuItem.setHeaderIcon(R.drawable.folder);

        SubMenu sharedWorkspacesMenuItem = menu.findItem(R.id.shared_workspaces).getSubMenu();
        sharedWorkspacesMenuItem.clear();

        if (this.serverCells) {
            sharedWorkspacesMenuItem.setHeaderTitle(R.string.shared_workspaces_title);
        } else {
            sharedWorkspacesMenuItem.setHeaderTitle(R.string.cells_litst_title);
        }
        sharedWorkspacesMenuItem.setHeaderIcon(R.drawable.share);

        for (String k : this.sortedKeys) {
            WorkspaceNode wn = data.get(k);
            if (wn == null) {
                continue;
            }

            final MenuItem item;
            if (wn.isShared()) {
                item = sharedWorkspacesMenuItem.add(R.id.drawer_shared_workspaces_section, 0, 0, wn.label());
                if (serverCells) {
                    item.setIcon(R.drawable.cells);
                } else {
                    item.setIcon(R.drawable.share);
                }
            } else {
                item = myWorkspaceMenuItem.add(R.id.drawer_my_workspace_section, 0, 0, wn.label());
                if ("personal-files".equals(wn.slug())) {
                    item.setIcon(R.drawable.ic_folder_account_grey600_48dp);
                } else {
                    item.setIcon(R.drawable.folder);
                }
            }

            if (Application.customTheme() != null) {
                SpannableString spanString = new SpannableString(item.getTitle().toString());
                spanString.setSpan(new ForegroundColorSpan(Application.customTheme().getMainColor()), 0, spanString.length(), 0);
                item.setTitle(spanString);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    item.setIconTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
                }
                Drawable d = item.getIcon();
                if (d != null) {
                    d.mutate().setColorFilter(Application.customTheme().getMainColor(), PorterDuff.Mode.SRC_IN);
                    item.setIcon(d);
                }
            }

            item.setActionView(R.layout.view_secondray_action_layout);
            final View actionView = item.getActionView();

            changeCountHandlers.put(wn.id(), (c) -> {
                LinearLayout syncInfoLayout = actionView.findViewById(R.id.sync_info_layout);
                if (c > 0) {
                    actionView.setVisibility(View.VISIBLE);
                    syncInfoLayout.setVisibility(View.VISIBLE);
                    TextView tv = actionView.findViewById(R.id.sync_changes_count_textview);
                    tv.setText(String.format("%d", c));
                } else {
                    syncInfoLayout.setVisibility(View.GONE);
                }
            });

            MenuItemSelectedActionView selectionActionView = new MenuItemSelectedActionView(actionView);
            selectionActionView.setDeselectedVisible(false);
            if (this.current == null || !wn.id().equals(this.current)) {
                selectionActionView.setSelected(false);
            } else {
                selectionActionView.setSelected(true);
            }


            item.setOnMenuItemClickListener((i) -> {
                if (this.completion != null) {
                    completion.onComplete(wn);
                }
                return true;
            });
        }
    }

    private void setSelected(String id) {

    }

    @Override
    public View getView() {
        return rootView;
    }
}

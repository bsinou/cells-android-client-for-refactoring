package com.pydio.android.client.gui.components;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.navigation.NavigationView;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.callback.Completion;

import java.util.List;


public class SessionListComponent extends Component {


    private String currentSession;

    private View rootView;
    private List<Session> data;
    private Completion<Session> selectedCompletion;

    private Runnable onNewRunnable;

    public SessionListComponent(View rootView) {
        this.rootView = rootView;
        initView();
        hide();
    }

    //****************************************************************************
    //          INIT
    //****************************************************************************
    protected void initView() {
    }

    //****************************************************************************
    //          DATA
    //****************************************************************************
    public void setData(String id, List<Session> sessions) {
        this.currentSession = id;
        this.data = sessions;
        populate();
    }

    void populate() {
        NavigationView navigationView = (NavigationView) rootView;
        Menu menu = navigationView.getMenu();
        menu.clear();

        for (Session s : this.data) {
            boolean currentSection = this.currentSession.equals(s.id());
            String label = String.format("%s@%s", s.user, s.server.url().replace("https://", "").replace("http://", ""));
            MenuItem item = menu.add(label);
            item.setActionView(R.layout.view_secondray_action_layout);

            View actionView = item.getActionView();
            MenuItemSelectedActionView selectionActionView = new MenuItemSelectedActionView(actionView);
            selectionActionView.setDeselectedVisible(false);
            if (currentSection) {
                selectionActionView.setSelected(true);
            } else {
                selectionActionView.setSelected(false);
            }
            item.setIcon(R.drawable.person);
            item.setOnMenuItemClickListener((v) -> enterSession(s));
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
        }

        MenuItem item = menu.add(R.string.new_account);
        item.setIcon(R.drawable.ic_account_plus_grey600_48dp);
        item.setOnMenuItemClickListener((v) -> {
            if (this.onNewRunnable != null) {
                this.onNewRunnable.run();
            }
            return true;
        });
    }

    //****************************************************************************
    //          ACTIONS
    //****************************************************************************
    private boolean enterSession(Session s) {
        if (selectedCompletion != null) {
            selectedCompletion.onComplete(s);
        }
        return true;
    }

    public void setSelectionCompletion(Completion<Session> c) {
        selectedCompletion = c;
    }

    public void setOnNewAccountButtonClicked(Runnable r) {
        this.onNewRunnable = r;
    }

    @Override
    public View getView() {
        return rootView;
    }
}

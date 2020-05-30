package com.pydio.android.client.gui.components;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Resources;
import com.pydio.android.client.gui.menu.models.ActionData;
import com.pydio.android.client.gui.menu.ListItemMenuData;

public class ListItemMenuComponent {

    private int rootHeight;

    private LinearLayout menuLayout;
    private View rootView;
    private NavigationView navigationView;
    private ListItemMenuData data;

    private ImageView headerIcon, headerOptionIcon;
    private TextView headerTitle;

    private int menuLayoutHeight;
    private int maxContainerHeight;

    private int currentY;
    public Runnable onShow;
    public Runnable onHide;

    private long animDuration = 450;
    private boolean showing = false;

    public ListItemMenuComponent(View view) {
        this.rootView = view;
        initView();
    }
    //private LinearLayout.LayoutParameters containerLayoutParams;

    public void setData(ListItemMenuData data) {
        this.data = data;
        populate();
    }

    private void initView() {

        rootView.setClickable(true);
        rootView.setOnTouchListener((v, event) -> {
            ListItemMenuComponent.this.hide();
            rootView.performClick();
            return false;
        });

        menuLayout = rootView.findViewById(R.id.list_item_menu_content);
        //containerLayoutParams = (LinearLayout.LayoutParameters) menuLayout.getLayoutParams();

        headerIcon = rootView.findViewById(R.id.header_icon);
        headerOptionIcon = rootView.findViewById(R.id.header_option_icon);
        headerTitle = rootView.findViewById(R.id.header_title);
        navigationView = rootView.findViewById(R.id.navigation_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            navigationView.setElevation(0);
        }
    }

    private void populate() {
        headerTitle.setText(data.label);
        headerIcon.setImageResource(data.resIcon);
        Drawable d = Resources.drawable(rootView.getContext(), data.resIcon, Resources.iconColor(data.resIcon));
        headerIcon.setImageDrawable(d);

        if (data.hasOption) {
            headerOptionIcon.setOnClickListener(data.infoClickListener);
        } else {
            headerOptionIcon.setVisibility(View.INVISIBLE);
            headerOptionIcon.setOnClickListener(null);
        }

        Menu menu = navigationView.getMenu();
        menu.clear();

        int itemCount = 0;
        if (data.actions.size() > 0) {
            itemCount += data.actions.size();
        }

        menuLayoutHeight = (int) rootView.getResources().getDimension(R.dimen.list_menu_item_header_height) + 6;
        menuLayoutHeight += (int) (itemCount * rootView.getResources().getDimension(R.dimen.browser_menu_item_height));

        int ordId = 1;
        int groupId = 1;
        int itemId = 1;

        for (ActionData ad : data.actions) {

            Menu m = menu;
            boolean hasSubMenus = ad.subActions != null && ad.subActions.size() > 0;
            if (hasSubMenus) {
                groupId++;
                m = menu.addSubMenu(groupId, itemId, ordId, ad.header);
                itemId++;

                MenuItem item = m.add(groupId, itemId, 1, ad.name);
                itemId++;

                item.setIcon(ad.iconResource);
                if (ad.withToggle) {
                    item.setActionView(R.layout.menu_item_action_view_toggle);
                    View view = item.getActionView();
                    Switch toggle = view.findViewById(R.id.toggle);
                    toggle.setChecked(ad.toggled);
                    toggle.setOnCheckedChangeListener(ad.toggleListener);
                    item.setOnMenuItemClickListener((mn) -> {
                        toggle.setChecked(!toggle.isChecked());
                        return true;
                    });
                } else {
                    item.setOnMenuItemClickListener(ad.clickListener);
                }

                for (int i = 0; i < ad.subActions.size(); i++) {
                    ActionData sad = ad.subActions.get(i);

                    MenuItem mi = m.add(groupId, itemId, i+2, sad.name);
                    itemId++;

                    mi.setIcon(sad.iconResource);
                    if (sad.withToggle) {
                        mi.setActionView(R.layout.menu_item_action_view_toggle);
                        View view = mi.getActionView();
                        Switch toggle = view.findViewById(R.id.toggle);
                        toggle.setChecked(ad.toggled);
                        toggle.setOnCheckedChangeListener(sad.toggleListener);
                        mi.setOnMenuItemClickListener((mn) -> {
                            toggle.setChecked(!toggle.isChecked());
                            return true;
                        });
                    } else {
                        mi.setOnMenuItemClickListener(sad.clickListener);
                    }
                }

            } else {
                MenuItem item = m.add(groupId, itemId, ordId, ad.name);
                itemId++;

                item.setIcon(ad.iconResource);
                if (ad.withToggle) {
                    item.setActionView(R.layout.menu_item_action_view_toggle);
                    View view = item.getActionView();
                    Switch toggle = view.findViewById(R.id.toggle);
                    toggle.setChecked(ad.toggled);
                    toggle.setOnCheckedChangeListener(ad.toggleListener);
                    item.setOnMenuItemClickListener((mn) -> {
                        toggle.setChecked(!toggle.isChecked());
                        return true;
                    });
                } else {
                    item.setOnMenuItemClickListener(ad.clickListener);
                }
            }
            ordId++;
        }
        animDuration = 200;
        menuLayout.getMeasuredHeight();
        menuLayout.requestLayout();
    }

    public void setContainerHeight(int height) {
        rootHeight = height;
        maxContainerHeight = 2 * (height / 3);
    }

    public boolean isShowing() {
        return showing;
    }

    public void show() {
        if (showing) {
            return;
        }
        showing = true;
        rootView.setVisibility(View.VISIBLE);
        rootView.requestLayout();
        final int menuHeight = menuLayout.getMeasuredHeight();
        currentY = (int) (rootHeight + menuHeight - menuLayout.getY());
        slideUp(menuLayout, rootHeight);
    }

    public void hide() {
        if (!showing) {
            return;
        }
        showing = false;
        rootView.setVisibility(View.VISIBLE);
        rootView.requestLayout();
        slideDown(menuLayout, currentY + rootHeight);
    }

    private void slideUp(View view, int to) {
        view.setVisibility(View.VISIBLE);

        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                to,  // fromYDelta
                0);                // toYDelta
        animate.setDuration(animDuration);
        animate.setFillAfter(true);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (ListItemMenuComponent.this.onShow != null) {
                    ListItemMenuComponent.this.onShow.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(animate);
    }

    // slide the view from its current position to below itself
    private void slideDown(View view, int to) {
        TranslateAnimation animate = new TranslateAnimation(
                0,// fromXDelta
                0,  // toXDelta
                0,// fromYDelta
                to);        // toYDelta
        animate.setDuration(animDuration);
        animate.setFillAfter(true);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                rootView.setVisibility(View.GONE);
                rootView.requestLayout();
                if (ListItemMenuComponent.this.onHide != null) {
                    ListItemMenuComponent.this.onHide.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(animate);
    }
}

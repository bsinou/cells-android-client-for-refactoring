package com.pydio.android.client.gui.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.callback.StringCompletion;
import com.pydio.android.client.gui.menu.models.ActionData;

import java.util.List;

public class ActionBarComponent extends Component {

    // Views
    private View rootView;
    private TextView title;
    private ImageView homeIcon;
    private LinearLayout first, second, actionGroup;
    private PopupMenu popupMenu;
    private RelativeLayout frontLayout, backLayout;
    private ImageView firstIcon, secondIcon;

    private TextInputLayout searchInputLayout;
    private TextInputEditText searchInputEdit;
    private ImageView searchIcon, searchActionIcon;
    private LinearLayout searchClearActionLayout;

    // Menu Data
    private List<ActionData> menuItemsData;

    // Sub components
    private TextIconComponent homeTextComponent;

    // Res
    private int backgroundColor;
    private int textColor;

    // Context
    private Context context;

    // Events
    private View.OnClickListener homeClickedListener;
    private StringCompletion searchInputCompletion;

    public ActionBarComponent(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
        this.frontLayout = rootView.findViewById(R.id.custom_action_bar_content);
        this.backLayout = rootView.findViewById(R.id.search_form_layout);
        this.title = rootView.findViewById(R.id.custom_action_bar_title);
        this.homeIcon = rootView.findViewById(R.id.custom_action_bar_button_home_icon);
        rootView.findViewById(R.id.custom_action_bar_home_button).setOnClickListener((v) -> {
            if (this.homeClickedListener != null) {
                this.homeClickedListener.onClick(v);
            }
        });
        this.actionGroup = rootView.findViewById(R.id.custom_action_bar_action_group);


        this.searchInputLayout = rootView.findViewById(R.id.search_input_layout);
        this.searchInputEdit = rootView.findViewById(R.id.search_input_edit_text);
        this.searchIcon = rootView.findViewById(R.id.search_icon);
        this.searchActionIcon = rootView.findViewById(R.id.search_action_icon);

        this.searchInputEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String searchText = s.subSequence(start, count).toString();
                    if (searchText.length() > 0) {
                        if (searchInputCompletion != null) {
                            searchInputCompletion.onComplete(searchText);
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        this.searchClearActionLayout = rootView.findViewById(R.id.search_clear_layout);
        this.searchClearActionLayout.setOnClickListener((v) -> {
            this.searchInputEdit.setText("");
            if (searchInputCompletion != null) {
                searchInputCompletion.onComplete("");
            }
        });

        this.first = rootView.findViewById(R.id.custom_action_bar_first_action_button);
        this.firstIcon = rootView.findViewById(R.id.custom_action_bar_first_action_icon);
        this.second = rootView.findViewById(R.id.custom_action_bar_second_action_button);
        this.secondIcon = rootView.findViewById(R.id.custom_action_bar_second_action_icon);

        View iconText = rootView.findViewById(R.id.action_bar_home_text_icon);
        this.homeTextComponent = new TextIconComponent(iconText);
        this.actionGroup.setVisibility(View.GONE);

        if (Application.customTheme() != null) {
            this.textColor = Application.customTheme().getSecondaryColor();
            this.backgroundColor = Application.customTheme().getMainColor();
        } else {
            this.backgroundColor = context.getResources().getColor(R.color.main_color);
            this.textColor = context.getResources().getColor(R.color.white);
        }

        this.title.setTextColor(textColor);
        this.homeIcon.setColorFilter(textColor);
        this.frontLayout.setBackgroundColor(this.backgroundColor);
        this.searchIcon.setColorFilter(textColor);
        this.searchActionIcon.setColorFilter(textColor);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void setTitle(int title) {
        this.title.setText(title);
    }

    public void setTextColor(int color) {
        this.textColor = color;
        this.homeIcon.setColorFilter(color);
        this.title.setTextColor(color);
    }

    public void setHomeIcon(int res) {
        this.homeIcon.setImageResource(res);
    }

    public void updateMenu(List<ActionData> items) {
        this.menuItemsData = items;
        if (items == null || items.size() == 0) {
            first.setVisibility(View.GONE);
            second.setVisibility(View.GONE);
            return;
        }
        first.setVisibility(View.VISIBLE);
        second.setVisibility(View.VISIBLE);

        this.actionGroup.setVisibility(View.VISIBLE);

        ActionData data = items.remove(0);
        if (data.iconBitmap != null) {
            firstIcon.setImageBitmap(data.iconBitmap);
        } else {
            firstIcon.setImageResource(data.iconResource);
        }
        firstIcon.setColorFilter(this.textColor);
        first.setTag(data.tag);
        ActionData finalData = data;
        first.setOnClickListener((v) -> finalData.clickListener.onMenuItemClick(null));


        int count = items.size();
        if (count == 0) {
            second.setVisibility(View.GONE);
            return;

        } else if (count == 1) {
            second.setVisibility(View.VISIBLE);
            data = items.remove(0);
            if (data.iconBitmap != null) {
                secondIcon.setImageBitmap(data.iconBitmap);
            } else {
                secondIcon.setImageResource(data.iconResource);
            }
            secondIcon.setColorFilter(this.textColor);
            second.setTag(data.tag);
            ActionData finalData1 = data;
            second.setOnClickListener((v) -> finalData1.clickListener.onMenuItemClick(null));
            return;
        }

        secondIcon.setImageResource(R.drawable.ic_dots_vertical_grey600_48dp);
        second.setOnClickListener((v) -> popupMenu.show());

        popupMenu = new PopupMenu(context, second);
        popupMenu.inflate(R.menu.action_bar_menu);

        Menu menu = popupMenu.getMenu();
        int order = 1;
        for (ActionData item : items) {
            MenuItem menuItem;
            if (item.group > 0) {
                menuItem = menu.add(item.group, 0, order, item.name);
            } else {
                menuItem = menu.add(item.name);
            }

            if (item.iconBitmap != null) {
                menuItem.setIcon(new BitmapDrawable(this.context.getResources(), item.iconBitmap));
            } else {
                menuItem.setIcon(item.iconResource);
            }
            menuItem.setOnMenuItemClickListener(item.clickListener);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                menuItem.setIconTintList(ColorStateList.valueOf(Application.customTheme().getSecondaryColor()));
            }
        }
    }

    private void refreshMenu() {
        this.updateMenu(this.menuItemsData);
    }

    public void hideHomeIcon() {
        View v = (View) homeIcon.getParent();
        v.setVisibility(View.INVISIBLE);
    }

    public void shoHomeIcon() {
        View v = (View) homeIcon.getParent();
        v.setVisibility(View.VISIBLE);
    }

    public void turnUncoloredMode() {
        this.backgroundColor = context.getResources().getColor(R.color.white);
        if (Application.customTheme() != null) {
            this.textColor = Application.customTheme().getMainColor();
        } else {
            this.textColor = context.getResources().getColor(R.color.main_color);
        }

        this.title.setTextColor(textColor);
        this.homeIcon.setColorFilter(textColor);
        this.frontLayout.setBackgroundColor(this.backgroundColor);
        this.searchIcon.setColorFilter(textColor);
        this.searchActionIcon.setColorFilter(textColor);
        this.refreshMenu();
    }

    public void turnColoredMode() {
        if (Application.customTheme() != null) {
            this.textColor = Application.customTheme().getSecondaryColor();
            this.backgroundColor = Application.customTheme().getMainColor();
        } else {
            this.backgroundColor = context.getResources().getColor(R.color.main_color);
            this.textColor = context.getResources().getColor(R.color.white);
        }

        this.title.setTextColor(textColor);
        this.homeIcon.setColorFilter(textColor);
        this.frontLayout.setBackgroundColor(this.backgroundColor);
        this.searchIcon.setColorFilter(textColor);
        this.searchActionIcon.setColorFilter(textColor);
        this.refreshMenu();
    }

    public void onHomeButtonClicked(View.OnClickListener listener) {
        homeClickedListener = listener;
    }

    public void requestSearch(StringCompletion c) {
        this.frontLayout.setVisibility(View.GONE);
        this.backLayout.setVisibility(View.VISIBLE);
        this.searchInputLayout.setVisibility(View.VISIBLE);
        this.searchInputCompletion = c;
    }

    public void cancelSearch() {
        if (this.frontLayout.getVisibility() == View.GONE) {
            this.searchInputLayout.setVisibility(View.GONE);
            this.backLayout.setVisibility(View.GONE);
            this.frontLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View getView() {
        return rootView;
    }
}

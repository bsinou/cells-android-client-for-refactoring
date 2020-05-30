package com.pydio.android.client.gui.menu.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.MenuItem;
import android.widget.CompoundButton;

import com.pydio.android.client.R;
import com.pydio.sdk.core.api.p8.consts.Action;

import java.util.ArrayList;
import java.util.List;

public class ActionData {

    public String header;
    public List<ActionData> subActions;

    public String name;
    public int iconResource;
    public Bitmap iconBitmap;
    public String tag;
    public boolean selected;
    public boolean withToggle;
    public boolean toggled;
    public int group;
    public MenuItem.OnMenuItemClickListener clickListener;
    public CompoundButton.OnCheckedChangeListener toggleListener;

    public static ActionData link(Context c, boolean enabled, CompoundButton.OnCheckedChangeListener toggleListener, MenuItem.OnMenuItemClickListener copyActionListener) {
        ActionData data = new ActionData();
        data.tag = c.getString(R.string.tag_public_link);
        if (enabled) {
            data.iconResource = R.drawable.ic_link_off_grey600_48dp;
            data.name = c.getString(R.string.disable);
        } else {
            data.iconResource = R.drawable.ic_link_grey600_48dp;
            data.name = c.getString(R.string.public_link);
        }
        data.withToggle = true;
        data.toggled = enabled;
        data.toggleListener = toggleListener;

        if (enabled) {
            data.header = c.getString(R.string.share_link);
            data.subActions = new ArrayList<>();
            data.subActions.add(linkCopy(c, copyActionListener));
        }
        return data;
    }

    public static ActionData linkCopy(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.copy;
        data.tag = c.getString(R.string.tag_copy_link);
        data.name = c.getString(R.string.copy_link);
        data.clickListener = listener;
        return data;
    }

    public static ActionData deleteLink(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.ic_link_off_grey600_48dp;
        data.tag = c.getString(R.string.tag_unshare);
        data.name = c.getString(R.string.delete_link);
        data.clickListener = listener;
        return data;
    }

    public static ActionData save(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.download;
        data.tag = c.getString(R.string.tag_save);
        data.name = c.getString(R.string.save);
        data.clickListener = listener;
        return data;
    }

    public static ActionData offline(Context c, boolean enabled, CompoundButton.OnCheckedChangeListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.baseline_offline_pin_black_48;
        data.tag = c.getString(R.string.tag_offline);
        data.name = c.getString(R.string.offline);
        data.withToggle = true;
        data.toggled = enabled;
        data.toggleListener = listener;
        return data;
    }

    public static ActionData delete(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.ic_trash_can_outline_grey600_48dp;
        data.tag = c.getString(R.string.tag_delete);
        data.name = c.getString(R.string.delete);
        data.clickListener = listener;
        return data;
    }

    public static ActionData move(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.ic_folder_move_grey600_48dp;
        data.tag = c.getString(R.string.tag_move);
        data.name = c.getString(R.string.move);
        data.clickListener = listener;
        return data;
    }

    public static ActionData copy(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.ic_content_copy_grey600_48dp;
        data.tag = c.getString(R.string.tag_copy);
        data.name = c.getString(R.string.copy);
        data.clickListener = listener;
        return data;
    }

    public static ActionData send(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.send;
        data.tag = c.getString(R.string.tag_send);
        data.name = c.getString(R.string.send);
        data.clickListener = listener;
        return data;
    }

    public static ActionData rename(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.rename;
        data.tag = c.getString(R.string.tag_rename);
        data.name = c.getString(R.string.rename);
        data.clickListener = listener;
        return data;
    }

    public static ActionData newFolder(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.folder;
        data.tag = c.getString(R.string.tag_folder);
        data.name = c.getString(R.string.folder);
        data.clickListener = listener;
        return data;
    }

    public static ActionData imports(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.image_plus;
        data.tag = c.getString(R.string.tag_import);
        data.name = c.getString(R.string.import_files);
        data.clickListener = listener;
        return data;
    }

    public static ActionData camera(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.camera;
        data.tag = c.getString(R.string.tag_camera);
        data.name = c.getString(R.string.upload_shoot);
        data.clickListener = listener;
        return data;
    }

    public static ActionData actionRefresh(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.refresh;
        data.tag = c.getString(R.string.tag_refresh);
        data.name = c.getString(R.string.refresh);
        data.clickListener = listener;
        return data;
    }

    public static ActionData actionBarList(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.list;
        data.tag = c.getString(R.string.tag_list);
        data.name = c.getString(R.string.list);
        data.clickListener = listener;
        return data;
    }

    public static ActionData actionBarGrid(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.grid;
        data.tag = c.getString(R.string.tag_grid);
        data.name = c.getString(R.string.grid);
        data.clickListener = listener;
        return data;
    }

    public static ActionData actionBarSearch(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.search;
        data.tag = c.getString(R.string.tag_search);
        data.name = c.getString(R.string.search);
        data.clickListener = listener;
        return data;
    }

    public static ActionData actionBarSortSize(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.tag = c.getString(R.string.tag_size);
        data.name = c.getString(R.string.sort_by_size);
        data.clickListener = listener;
        return data;
    }

    public static ActionData actionBarSortLastModified(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.tag = c.getString(R.string.tag_modified);
        data.name = c.getString(R.string.sort_by_modif);
        data.clickListener = listener;
        return data;
    }

    public static ActionData actionBarSortType(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.tag = c.getString(R.string.tag_type);
        data.name = c.getString(R.string.sort_by_modif);
        data.clickListener = listener;
        return data;
    }

    public static ActionData clearRecycleBin(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.ic_trash_can_outline_grey600_48dp;
        data.tag = c.getString(R.string.tag_clear_recycle);
        data.name = c.getString(R.string.empty_recycle);
        data.clickListener = listener;
        return data;
    }

    public static ActionData restore(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.ic_delete_restore_grey600_48dp;
        data.tag = c.getString(R.string.tag_restore);
        data.name = c.getString(R.string.restore);
        data.clickListener = listener;
        return data;
    }

    public static ActionData close(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.outline_close_black_48;
        data.tag = c.getString(R.string.tag_close);
        data.name = c.getString(R.string.close);
        data.clickListener = listener;
        return data;
    }

    public static ActionData bookmark(Context c, boolean toggled, CompoundButton.OnCheckedChangeListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.baseline_bookmark_black_48;
        data.withToggle = true;
        data.toggled = toggled;

        data.tag = c.getString(R.string.tag_bookmark);
        data.name = c.getString(R.string.bookmark);
        data.toggleListener = listener;
        return data;
    }

    public static ActionData options(Context c, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.iconResource = R.drawable.dots_vertical;
        data.tag = c.getString(R.string.tag_more);
        data.name = c.getString(R.string.options);
        data.clickListener = listener;
        return data;
    }

    public static ActionData selectAll(Context c, boolean selected, MenuItem.OnMenuItemClickListener listener) {
        ActionData data = new ActionData();
        data.selected = selected;
        if (selected) {
            data.iconResource = R.drawable.ic_checkbox_marked_circle_grey600_48dp;
        } else {
            data.iconResource = R.drawable.baseline_check_circle_outline_black_48;
        }
        data.tag = c.getString(R.string.tag_more);
        data.name = c.getString(R.string.options);
        data.clickListener = listener;
        return data;
    }
}

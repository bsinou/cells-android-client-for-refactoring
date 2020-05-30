package com.pydio.android.client.gui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Resources;
import com.pydio.android.client.data.nodes.NodeUtils;
import com.pydio.android.client.data.nodes.OfflineInfo;
import com.pydio.android.client.data.nodes.SelectionInfo;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Node;

public class ViewData {
    public String mainText;
    public String secondaryText;

    public Bitmap icon;
    public int iconRes;
    public Bitmap defaultIcon;
    public int iconColorFilter;
    public float iconScaleX, iconScaleY;

    public int shareFlagVisibility;
    public int syncFlagVisibility;
    public int starredFlagVisibility;

    public int optionsLayoutVisibility;
    public int treeDotsVisibility;

    public int selectedVisibility;
    public int unselectedVisibility;

    public int selectionLayoutVisibility;

    public View.OnClickListener optionClickListener;

    public boolean selectionMode, selected, hasOption;

    public ViewData setMainText(String text) {
        mainText = text;
        return this;
    }

    public ViewData setSecondaryText(String text) {
        secondaryText = text;
        return this;
    }

    public ViewData setDefaultIcon(int res) {
        iconRes = res;
        defaultIcon = BitmapFactory.decodeResource(Application.context().getResources(), res);
        return this;
    }

    public ViewData setIcon(Bitmap bmp) {
        icon = bmp;
        return this;
    }

    public ViewData setIconScale(float scaleX, float scaleY) {
        this.iconScaleX = scaleX;
        this.iconScaleY = scaleY;
        return this;
    }

    public ViewData setSelected(boolean selected) {
        if (selected) {
            unselectedVisibility = View.GONE;
            selectedVisibility = View.VISIBLE;
        } else {
            unselectedVisibility = View.VISIBLE;
            selectedVisibility = View.GONE;
        }
        return this;
    }

    public ViewData setShared(boolean isShared) {
        if (isShared) {
            shareFlagVisibility = View.VISIBLE;
        } else {
            shareFlagVisibility = View.GONE;
        }
        return this;
    }

    public ViewData setIsSynced(boolean synced) {
        if (synced) {
            syncFlagVisibility = View.VISIBLE;
        } else {
            syncFlagVisibility = View.GONE;
        }
        return this;
    }

    public ViewData setHasOption(boolean hasOption) {
        this.hasOption = hasOption;
        if (hasOption) {
            treeDotsVisibility = View.VISIBLE;
        } else {
            treeDotsVisibility = View.GONE;
        }
        return this;
    }

    public ViewData setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        if (selectionMode) {
            optionsLayoutVisibility = View.GONE;
            treeDotsVisibility = View.GONE;
            selectionLayoutVisibility = View.VISIBLE;
        } else {
            optionsLayoutVisibility = View.VISIBLE;
            treeDotsVisibility = View.VISIBLE;
            selectionLayoutVisibility = View.GONE;
        }
        return this;
    }

    public ViewData setIconColorFilter(int resColor) {
        this.iconColorFilter = resColor;
        return this;
    }

    public ViewData setStarred(boolean starred) {
        if (starred) {
            this.starredFlagVisibility = View.VISIBLE;
        } else {
            this.starredFlagVisibility = View.GONE;
        }
        return this;
    }

    public ViewData setOptionClickListener(View.OnClickListener listener) {
        optionClickListener = listener;
        return this;
    }

    public static ViewData parse(Context context, Node node, OfflineInfo offlineInfo, SelectionInfo selectionInfo) {
        boolean hasOption = !NodeUtils.isBookmarked(node);
        boolean isShared = NodeUtils.isShared(node);
        int iconRes = NodeUtils.iconResource(node);
        int iconColorFilter = Resources.iconColor(iconRes);
        boolean isImage = NodeUtils.isImage(node);
        boolean bookmarked = NodeUtils.isBookmarked(node);
        boolean shared = NodeUtils.isShared(node);
        boolean isOffline = offlineInfo.isWatched((FileNode) node);

        String secondaryText = "";
        long last_modified = NodeUtils.lastModified(node);
        if (last_modified != 0) {
            secondaryText = NodeUtils.lastModificationDate(context, last_modified * 1000);
        }

        long size = NodeUtils.size(node);
        if (size != 0) {
            if (secondaryText.length() > 0) {
                secondaryText += "  -  ";
            }
            secondaryText += NodeUtils.stringSize(size);
        }

        return new ViewData()
                .setDefaultIcon(iconRes)
                .setIconColorFilter(iconColorFilter)
                .setMainText(node.label())
                .setSecondaryText(secondaryText)
                .setShared(isShared)
                .setHasOption(hasOption)
                .setShared(shared)
                .setStarred(bookmarked)
                .setIsSynced(isOffline)
                .setSelectionMode(selectionInfo.inSelectionMode())
                .setSelected(selectionInfo.isSelected(node));
    }
}

package com.pydio.android.client.gui.components;

import android.os.Handler;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.pydio.android.client.data.images.ThumbLoader;
import com.pydio.android.client.data.listing.Filter;
import com.pydio.android.client.data.nodes.EventHandler;
import com.pydio.android.client.gui.view.group.OnContentLoadedListener;
import com.pydio.android.client.gui.view.group.OnEmptyContentActionHandler;
import com.pydio.android.client.gui.view.group.OnEmptyContentEventListener;
import com.pydio.sdk.core.model.Node;

import java.util.List;

import static android.os.Looper.getMainLooper;

public abstract class BrowserPage extends Component implements EventHandler {

    ClickListener clickListener;
    LongClickListener longClickListener;
    OnEmptyContentEventListener emptyContentListener;
    OnEmptyContentActionHandler emptyContentActionHandler;
    OnContentLoadedListener contentLoadedListener;
    ClickListener itemOptionClickedListener;
    ThumbLoader thumbLoader;

    int height, width;

    public static int ModeDefault = 0;
    public static int ModePaste = 1;
    public static int ModeSearch = 2;
    public static int ModeBookmark = 3;

    public void setItemClickListener(ClickListener listener) {
        this.clickListener = listener;
    }

    public void setItemLongClickListener(LongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setEmptyContentListener(OnEmptyContentEventListener emptyContentListener) {
        this.emptyContentListener = emptyContentListener;
    }

    public void setEmptyContentActionHandler(OnEmptyContentActionHandler emptyContentActionHandler) {
        this.emptyContentActionHandler = emptyContentActionHandler;
    }

    public void setContentLoadedListener(OnContentLoadedListener listener) {
        this.contentLoadedListener = listener;
    }

    public void setItemOptionsClickListener(ClickListener listener) {
        this.itemOptionClickedListener = listener;
    }

    public void setThumbnailLoader(ThumbLoader loader) {
        this.thumbLoader = loader;
    }

    public void enterSelectionMode() {

    }

    public void exitSelectionMode() {
    }

    public void enterPasteMode() {
    }

    public void exitPasteMode() {

    }

    public void setInBackground() {

    }

    public void setInForeground() {

    }

    public void searchRequest(String pattern) {
    }

    protected synchronized void showMessage(String message) {
        new Handler(getMainLooper()).post(() -> {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT);
            snackbar.show();
        });
    }

    protected synchronized void showMessage(String format, Object... params) {
        new Handler(getMainLooper()).post(() -> {
            final String message = String.format(format, params);
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT);
            snackbar.show();
        });
    }

    public void setHeight(int height) {
        View rootView = getView();
        this.height = height;
        rootView.getLayoutParams().height = height;
        rootView.requestLayout();
    }

    public void setWidth(int width) {
        View rootView = getView();
        this.width = width;
        rootView.getLayoutParams().width = width;
        rootView.requestLayout();
    }

    public int getWidth() {
        return getView().getMeasuredWidth();
    }

    public int getHeight() {
        return getView().getMeasuredHeight();
    }

    public abstract void loadContent();

    public abstract int mode();

    public abstract List<Node> get(Filter filter);

    public abstract Node itemAt(int index);

    public abstract void refresh();

    public abstract void displayModeChanged();

    public abstract int count();

    public abstract Node node();

    public interface ClickListener {
        void onClick(Node node);
    }

    public interface LongClickListener {
        void onLongClick(Node node);
    }
}

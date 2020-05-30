package com.pydio.android.client.gui.components;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;

import com.pydio.android.client.R;
import com.pydio.android.client.data.images.ThumbLoader;
import com.pydio.android.client.data.listing.ContentPageState;
import com.pydio.android.client.gui.view.group.OnContentLoadedListener;
import com.pydio.android.client.gui.view.group.OnEmptyContentActionHandler;
import com.pydio.android.client.gui.view.group.OnEmptyContentEventListener;
import com.pydio.sdk.core.model.Node;

import java.util.ArrayList;
import java.util.List;

public class NodeListComponent extends BrowserPage {

    private View rootView;
    private List<BrowserPage> history;
    private BrowserPage currentPage;

    private FrameLayout stack;

    private boolean selectionMode;
    private EmptyContentComponent emptyContentView;

    private ClickListener clickListener, optionClickListener;
    private LongClickListener longClickListener;
    private OnEmptyContentEventListener emptyContentListener;
    private OnContentLoadedListener contentLoadedListener;
    private OnEmptyContentActionHandler emptyContentActionHandler;

    private int width = 0, height = 0;
    public NodeListComponent(View rootView) {
        this.rootView = rootView;
        this.history = new ArrayList<>();
        initView();
    }


    //***********************************************************************
    //              INIT
    //***********************************************************************
    public void initView() {
        //initSwipeRefreshLayout();
        initViewGroup();
        initEmptyContent();
    }

    private void initEmptyContent() {
        View view = rootView.findViewById(R.id.empty_content_view);
        emptyContentView = new EmptyContentComponent(view);
        emptyContentView.setButtonClickListener((v) -> {
            emptyContentButtonClicked();
        });
    }

    private void initViewGroup() {
        stack = rootView.findViewById(R.id.page_stack);
        /*stack.setOnItemLongClickListener(this::itemLongClicked);
        stack.setOnItemClickListener(this::itemClicked);
        stack.setMetrics(this.metrics);
        viewType = R.layout.view_list_cell_layout;*/
        /*stack.setHorizontalSpace(listHorizontalSpacing);
        stack.setVerticalSpace(listVerticalSpacing);*/
    }

    @Override
    public int mode() {
        if (currentPage != null) {
            return currentPage.mode();
        }
        return BrowserPage.ModeDefault;
    }

    @Override
    public List<Node> get(com.pydio.android.client.data.listing.Filter filter) {
        return currentPage.get(filter);
    }
    @Override
    public Node itemAt(int index) {
        return null;
    }

    //***********************************************************************
    //              EVENT
    //***********************************************************************
    @Override
    public void setItemClickListener(ClickListener listener) {
        clickListener = listener;
        if(currentPage != null){
            currentPage.setItemClickListener(clickListener);
        }
    }
    @Override
    public void setItemLongClickListener(LongClickListener listener) {
        longClickListener = listener;
        if(currentPage != null){
            currentPage.setItemLongClickListener(longClickListener );
        }
    }
    @Override
    public void setEmptyContentListener(OnEmptyContentEventListener emptyContentListener) {
        this.emptyContentListener = emptyContentListener;
        if(currentPage != null){
            currentPage.setEmptyContentListener(this::emptyContentEvent);
        }
    }
    @Override
    public void setEmptyContentActionHandler(OnEmptyContentActionHandler emptyContentActionHandler) {
        this.emptyContentActionHandler = emptyContentActionHandler;
        if(currentPage != null){
            currentPage.setEmptyContentActionHandler(emptyContentActionHandler);
        }
    }
    @Override
    public void setContentLoadedListener(OnContentLoadedListener listener) {
        this.contentLoadedListener = listener;
        if(currentPage != null){
            currentPage.setContentLoadedListener(listener);
        }
    }
    @Override
    public void setItemOptionsClickListener(ClickListener listener) {
        this.optionClickListener = listener;
        if(currentPage != null){
            currentPage.setItemOptionsClickListener(listener);
        }
    }
    @Override
    public void setThumbnailLoader(ThumbLoader loader) {
        ThumbLoader thumbLoader = loader;
        if(currentPage != null){
            currentPage.setThumbnailLoader(loader);
        }
    }

    //***********************************************************************
    //              DATA
    //***********************************************************************
    public void refresh() {
        if(currentPage == null){
            stack.setVisibility(View.GONE);
            emptyContentView.getView().setVisibility(View.VISIBLE);
            emptyContentEvent();
        } else {
            emptyContentView.getView().setVisibility(View.GONE);
            stack.setVisibility(View.VISIBLE);
            currentPage.refresh();
        }
    }
    @Override
    public void displayModeChanged() {
        if(history == null){
            return;
        }

        for(BrowserPage page: history){
            page.displayModeChanged();
        }
    }

    @Override
    public int count() {
        if (currentPage == null){
            return 0;
        }
        return currentPage.count();
    }

    public Node node() {
        if(currentPage != null){
            return currentPage.node();
        }
        return null;
    }

    //***********************************************************************
    //              SUPER
    //***********************************************************************
    public View getView() {
        return rootView;
    }

    //************************************************************************
    //              CONTROL
    //************************************************************************
    public boolean inSelectionMode() {
        return selectionMode;
    }
    @SuppressLint("UseSparseArrays")
    public void enterSelectionMode() {
        selectionMode = true;
        currentPage.enterSelectionMode();
    }
    @Override
    public void exitSelectionMode() {
        if(currentPage != null){
            currentPage.exitSelectionMode();
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
        if(currentPage != null){
            currentPage.setInBackground();
        }
    }
    @Override
    public void setInForeground() {
        if(currentPage != null){
            currentPage.setInForeground();
        }
    }

    public void setEmptyContentText(String text) {
        emptyContentView.setText(text);
    }

    public void setEmptyContentText(int resText) {
        emptyContentView.setText(resText);
    }

    public void setEmptyContentButtonText(String text) {
        emptyContentView.setButtonText(text);
    }

    public void setEmptyButtonText(int resText) {
        emptyContentView.setButtonText(resText);
    }

    public void setViewWithFAB(boolean b) {
        /*if(b) {
            Resources res = rootView.getResources();
            int distance = (int) ( res.getDimension(R.dimen.list_cell_height) + res.getDimension(R.dimen.default_spacing)*2);
            stack.setScrollMargin(distance);
        } else {
            stack.setScrollMargin(0);
        }*/
    }


    //************************************************************************
    //              STACK
    //************************************************************************
    public void pushNew(ContentPageState state) {
        BrowserPage component = null;

        if (state.mode == ModeSearch) {
            component = new SearchFolderComponent(state);
        } else {
             component = new FolderPageComponent(state);
        }

        View view = component.getView();
        stack.addView(view);

        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", getWidth(), 0);
        animator.setDuration(200);
        animator.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animator) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    view.setElevation(2);
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();

        history.add(component);
        if(currentPage != null){
            currentPage.setInBackground();
        }
        currentPage = component;
        initPage(currentPage);
        currentPage.loadContent();
        //currentPage.setInForeground();
    }

    public void pop() {
        int index = history.size() - 1;
        if(index > 0) {
            //show page at the bottom of the top page
            final BrowserPage page = history.get(index -1);
            //page.setInForeground();

            history.remove(index);
            final View v = currentPage.getView();
            float x = v.getX();
            float toX = v.getMeasuredWidth() + x;

            ObjectAnimator animator = ObjectAnimator.ofFloat(v, "translationX", x, toX);
            animator.setDuration(200);
            animator.addListener(new Animator.AnimatorListener(){
                @Override
                public void onAnimationStart(Animator animator) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        v.setElevation(2);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    currentPage = page;
                    stack.removeViewAt(index);
                    initPage(currentPage);
                    currentPage.setInForeground();
                }

                @Override
                public void onAnimationCancel(Animator animator) {}

                @Override
                public void onAnimationRepeat(Animator animator) {}
            });
            /*animator.addUpdateListener(valueAnimator -> {
                float animatedValue = (float)valueAnimator.getAnimatedValue();
                v.setX(animatedValue);
            });*/
            animator.start();
        }
    }

    public boolean hasStackedView(){
        return history.size() > 1;
    }

    private void initPage(BrowserPage page){
        page.setContentLoadedListener(this.contentLoadedListener);
        page.setEmptyContentListener(this::emptyContentEvent);
        page.setEmptyContentActionHandler(this::emptyContentButtonClicked);
        page.setContentLoadedListener(this.contentLoadedListener);
        page.setItemClickListener(this.clickListener);
        page.setItemLongClickListener(this.longClickListener);
        page.setItemOptionsClickListener(this.optionClickListener);

        currentPage.setHeight(this.height);
        emptyContentView.getView().setVisibility(View.GONE);
        stack.setVisibility(View.VISIBLE);
    }

    public void clear(){
        if(this.stack != null){
            this.stack.removeAllViews();
        }

        if(this.history != null){
            this.history.clear();
        }
    }

    // Events
    public void emptyContentButtonClicked() {
        if (this.emptyContentActionHandler != null) {
            this.emptyContentActionHandler.onAction();
        }
    }

    public void emptyContentEvent() {
        if (this.emptyContentListener != null) {
            this.emptyContentListener.onEmptyContent();
        }
    }

    public void contentLoadedEvent(){
        if (this.contentLoadedListener != null) {
            this.contentLoadedListener.onContentLoaded();
        }
    }

    public void itemClicked(View view, int position) {}

    // View dimensions
    public void setHeight(int height){
        this.height =height;
        rootView.getLayoutParams().height = height;
        rootView.requestLayout();
        /*if(this.metrics != null){
            this.metrics.calculateItemsWidth(rootView.getLayoutParams().width);
        }*/
    }

    public void setWidth(int width){
        this.width = width;
        rootView.getLayoutParams().width = width;
        rootView.requestLayout();
        /*if(this.metrics != null){
            this.metrics.calculateItemsWidth(width);
        }*/
    }

    public int getHeight(){
        return rootView.getMeasuredHeight();
    }

    // Browser page
    @Override
    public void searchRequest(String pattern) {
        if (this.currentPage != null) {
            this.currentPage.searchRequest(pattern);
        }
    }
    @Override
    public void loadContent() {
        if (this.currentPage != null) {
            this.currentPage.loadContent();
        }
    }

    public int getWidth(){
        return rootView.getMeasuredWidth();
    }


    // EventHandler

    @Override
    public void onCreated(Node... nodes) {
        if(this.currentPage != null) {
            this.currentPage.onCreated(nodes);
        }
    }

    @Override
    public void onDeleted(Node... nodes) {
        if(currentPage != null) {
            this.currentPage.onDeleted(nodes);
        }
    }

    @Override
    public void onUpdated(Node... nodes) {
        if(currentPage != null) {
            this.currentPage.onUpdated(nodes);
        }
    }

    @Override
    public void onRenamed(Node node, String newName) {
        if(this.currentPage != null) {
            this.currentPage.onRenamed(node, newName);
        }
    }

    @Override
    public void onShared(Node node) {
        if(this.currentPage != null) {
            this.currentPage.onShared(node);
        }
    }

    @Override
    public void onUnShared(Node node) {
        if(this.currentPage != null) {
            this.currentPage.onUnShared(node);
        }
    }

    @Override
    public void onBookmarked(Node node) {
        if(this.currentPage != null) {
            this.currentPage.onBookmarked(node);
        }
    }

    @Override
    public void onUnBookmarked(Node node) {
        if(this.currentPage != null) {
            this.currentPage.onUnBookmarked(node);
        }
    }

    @Override
    public void onWatched(Node node) {
        if(this.currentPage != null) {
            this.currentPage.onWatched(node);
        }
    }

    @Override
    public void onUnWatched(Node node) {
        if(this.currentPage != null) {
            this.currentPage.onUnWatched(node);
        }
    }
}

package com.pydio.android.client.gui.view.group;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.core.view.MotionEventCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class ViewGroup extends android.view.ViewGroup implements View.OnTouchListener, Serializable {

    protected Context context;
    protected int totalDistanceScrolled = 0;
    protected int scrollRemainingDistance = 0;
    protected int scrollDifference = 0;
    protected int scrollMargin = 0;

    protected int slop;
    protected int minFlingVelocity;
    protected int maxFlingVelocity;

    protected float downX, downY, previousMoveY, previousMoveX;
    protected View touchedChild;
    protected boolean hasMovedY, hasMovedX;

    protected float velocityY, velocityX;
    protected VelocityTracker tracker;
    protected Scroller scroller;
    protected float deltaY, deltaX, totalDeltaX, totalDeltaY;

    protected boolean isTouchIntercepted;

    boolean scrollingDown, scrollingUp;

    boolean canSwipeDown = false, swipeDown = false;
    boolean touchedViewPressed = false, touchedViewReleased = false;
    Drawable touchedViewDrawable;

    int topViewIndex = 0,
            bottomViewIndex = 0,
            visibleViewCount = 0,
            bottomViewX = 0,
            bottomViewY = 0;

    ViewGroupAdapter adapter;

    public static long LONG_PRESS_MIN_DURATION = 1000;
    boolean longPressed = false, longPressedHandlerCalled = false;
    OnItemClickedListener itemClickedListener;
    OnItemLongClickedListener itemLongClickedListener;

    boolean mHasMoved, mFinished, mClickEffectsEnabled = true, mLongClickEnabled = true;
    Timer mLongClickTimer;

    int mContentHeight;
    int mScrollMargin = 0;

    List<View> mRecycledTypedView;

    Map<Long, Pair<Integer, Integer>> mSavedPositions;
    Map<Long, Pair<Integer, Integer>> mSavedDimensions;

    List<Integer> mXs;
    List<Integer> mYs;
    List<LayoutParameters> lParams;

    OnClickListener mClickListener;

    Metrics metrics;

    public ViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        ViewConfiguration mViewConfig = ViewConfiguration.get(context);
        slop = mViewConfig.getScaledTouchSlop();
        minFlingVelocity = mViewConfig.getScaledMinimumFlingVelocity();
        maxFlingVelocity = mViewConfig.getScaledMaximumFlingVelocity();

        mRecycledTypedView = new ArrayList<>();
        mSavedPositions = new HashMap<>();
        mSavedDimensions = new HashMap<>();

        mContentHeight = 0;
        mXs = new ArrayList<>();
        mYs = new ArrayList<>();
        lParams = new ArrayList<>();

        setOnTouchListener(this);
        setClickable(true);

        setVerticalScrollBarEnabled(true);
        setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setScrollBarDefaultDelayBeforeFade(1500);
        }
        setWillNotDraw(false);
    }

    //**********************************************************************
    //              Layout and positions
    //**********************************************************************
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.AT_MOST);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), getDefaultSize(this.getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getMeasuredWidth();

        int childCount = getChildCount();
        bottomViewIndex = topViewIndex + childCount - 1;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() != GONE) {

                int itemWidth, itemHeight;
                LayoutParameters lp = lParams.get(topViewIndex + i);
                itemWidth = lp.width;
                itemHeight = lp.height;

                int left = mXs.get(i + topViewIndex);
                int top = mYs.get(i + topViewIndex);

                if (itemWidth == LayoutParams.MATCH_PARENT) {
                    itemWidth = width - left - lp.rightSpace;
                }

                int right = left + itemWidth;
                int bottom = top + itemHeight;
                child.measure(MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(itemHeight, MeasureSpec.EXACTLY));
                child.layout(left, top, right, bottom);
            }
        }
    }

    public int visibleHeight() {
        return ((View) getParent()).getMeasuredHeight();
    }

    public int calculateHeightAndChildrenPositions() {
        mXs.clear();
        mYs.clear();

        if (adapter == null) return 0;
        mRecycledTypedView.clear();

        int width = getMeasuredWidth();
        int itemWidth, itemHeight, previousHeight = 0;

        int curX = 0, curY = 0, lineItemsCount = 0;
        int dataCount = adapter.getCount();

        for (int i = 0; i < dataCount; i++) {
            int type = adapter.getItemViewType(i);
            LayoutParameters lp = adapter.getLayoutParams(type);
            lParams.add(lp);

            itemWidth = lp.leftSpace + lp.width + lp.rightSpace;
            itemHeight = lp.topSpace + lp.height + lp.bottomSpace;

            boolean newLine = i > 0 &&
                    (itemWidth == LayoutParams.MATCH_PARENT && curX == width
                            || curX + itemWidth > width);

            if (newLine) {
                lineItemsCount = 0;
                curX = 0;
                curY += previousHeight;
            }

            if (itemWidth == LayoutParams.MATCH_PARENT) {
                itemWidth = width - curX;
            }

            mXs.add(curX);
            mYs.add(curY);
            curX += itemWidth;

            if (curX == width) {
                curX = 0;
                curY += itemHeight;
                previousHeight = 0;
                lineItemsCount = 0;
            } else {
                lineItemsCount++;
                previousHeight = itemHeight;
            }
        }

        if (lineItemsCount > 0) {
            curY += previousHeight;
        }
        return mContentHeight = curY + mScrollMargin;
    }

    private void recycleView(View v, int type) {
        if (mRecycledTypedView.size() > 10) {
            return;
        }
        mRecycledTypedView.add(v);
    }

    private View getRecycledView(int type) {
        int size = mRecycledTypedView.size();
        if (size == 0) {
            return null;
        }
        return mRecycledTypedView.remove(size -1);
    }

    private void saveState() {
        mSavedPositions.clear();
        mSavedDimensions.clear();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            long id = adapter.getItemId(topViewIndex + i);
            if (v.getY() > totalDistanceScrolled) {
                mSavedPositions.put(id, new Pair<>((int) v.getX(), (int) v.getY()));
                mSavedDimensions.put(id, new Pair<>(v.getMeasuredWidth(), v.getMeasuredHeight()));
            }
        }
    }

    public void setAdapter(ViewGroupAdapter a) {
        adapter = a;
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                saveState();
                calculateHeightAndChildrenPositions();
                refresh();
            }

            @Override
            public void onInvalidated() {
                saveState();
                calculateHeightAndChildrenPositions();
                refresh();
            }
        });
    }

    void refresh() {
        setEnabled(false);
        topViewIndex = scrollDifference = visibleViewCount = 0;
        totalDistanceScrolled = 0;

        bottomViewIndex = 0;
        bottomViewX = 0;
        bottomViewY = 0;

        int count = adapter.getCount(),
                visiblePartFromTopIndex = 0,
                visibleHeight = visibleHeight(),
                visibleViewCount = 0;
        int lineItemCount = 0;


        /*
         * based on the intersection between the previous content and the current one
         * we calculate the position of the first item to display
         */
        int i = 0;
        boolean foundTop = false, contentHeightReached = false;

        while (i < count) {
            long id = adapter.getItemId(i);
            if (!foundTop && mSavedPositions.containsKey(id)) {
                topViewIndex = i - lineItemCount;
                foundTop = true;
            }

            contentHeightReached = visiblePartFromTopIndex > visibleHeight;

            if (!contentHeightReached) {
                visiblePartFromTopIndex = mYs.get(i);
                visibleViewCount++;
            }

            if (contentHeightReached && foundTop) {
                break;
            }

            i++;
        }


        if (topViewIndex > 0) {
            /*
             * then if the intersection is not empty we add make sure the displayed content height
             * fit the screen. If not
             * */
            totalDistanceScrolled = mYs.get(topViewIndex);

            //contentVisibleHeight = mContentHeight - totalDistanceScrolled - visibleHeight;
            visiblePartFromTopIndex = mContentHeight - totalDistanceScrolled;
            int curX, prevY = mYs.get(topViewIndex);

            while (topViewIndex > 0) {
                int index = topViewIndex - 1;
                int y = mYs.get(index);
                curX = mXs.get(index);
                topViewIndex--;
                if (curX == 0) {
                    if (visiblePartFromTopIndex >= visibleHeight) {
                        break;
                    }
                    visiblePartFromTopIndex += prevY - y;
                }
                visibleViewCount++;
                totalDistanceScrolled = y;
            }

        } else {
            topViewIndex = 0;
            totalDistanceScrolled = 0;
        }
        removeAllViews();

        bottomViewIndex = 0;
        for (i = 0; i < visibleViewCount; i++) {
            int type = adapter.getItemViewType(i);
            int index = i + topViewIndex;

            if (index < count) {
                final View v = adapter.getView(index, getRecycledView(type), this);
                bottomViewIndex++;
                addView(v);
                //TODO: animate newly added
            }
        }

        mSavedPositions.clear();
        mSavedDimensions.clear();

        this.visibleViewCount = visibleViewCount + 1;
        setEnabled(true);
        scrollTo(0, totalDistanceScrolled);
        requestLayout();
    }

    //**********************************************************************
    //              SPACING
    //**********************************************************************
    public void setMetrics(Metrics m) {
        this.metrics = m;
    }

    //**********************************************************************
    //              SCROLLING AND MOVES
    //**********************************************************************
    @Override
    protected int computeVerticalScrollExtent() {
        if (getHeight() == 0) return 0;
        return visibleHeight() * visibleHeight() / getHeight();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        if (getHeight() == 0) return 0;
        return scrolledDistance() * visibleHeight() / getHeight();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return getHeight();
    }

    private int scrollUp(int distance) {
        //pull down
        if (topViewIndex == 0) {
            return distance;
        }

        int topViewPreviousItemIndex = topViewIndex - 1;
        LayoutParameters plp;

        for(;;) {
            int type = adapter.getItemViewType(topViewPreviousItemIndex);
            View previousView = adapter.getView(topViewPreviousItemIndex, getRecycledView(type), this);
            plp =  lParams.get(topViewPreviousItemIndex);

            final int height = plp.topSpace = plp.height + plp.bottomSpace;

            topViewIndex = topViewPreviousItemIndex;
            addView(previousView, 0);

            if (topViewPreviousItemIndex == 0) break;
            topViewPreviousItemIndex--;

            if(distance >= height){
                break;
            }
        }


        int groupMeasuredHeight = getMeasuredHeight();
        int bottomLimit = totalDistanceScrolled + groupMeasuredHeight;

        while (bottomViewY > bottomLimit) {
            // we do not need to update bottomViewY value
            // the onLayout method will do it for us
            bottomViewIndex--;
            removeViewAt(getChildCount() - 1);
        }
        return distance;
    }

    private int scrollDown(int distance) {
        //pull up

        int count = adapter.getCount();
        int bottomViewNextItemIndex = bottomViewIndex + 1;

        int currentLineY;

        try {
            currentLineY = mYs.get(bottomViewIndex);
        }catch (IndexOutOfBoundsException ignored){return distance;}

        LayoutParameters nlp;

        while (bottomViewNextItemIndex < count) {

            int type = adapter.getItemViewType(bottomViewNextItemIndex);
            View nextView = adapter.getView(bottomViewNextItemIndex, getRecycledView(type), this);
            nlp = lParams.get(bottomViewNextItemIndex);

            int y = mYs.get(bottomViewNextItemIndex);

            if (y > currentLineY) {
                if (distance < +nlp.topSpace + nlp.height + nlp.bottomSpace) {
                    break;
                }
                distance -= y - currentLineY;
                currentLineY = y;
            }
            bottomViewNextItemIndex++;
            bottomViewIndex++;
            addView(nextView);
        }

        int topLimit = totalDistanceScrolled, itemHeight;

        while (getChildCount() > visibleViewCount) {
            View v = getChildAt(0);
            itemHeight = v.getMeasuredHeight();

            int TopY = mYs.get(topViewIndex);
            if (TopY + itemHeight < topLimit) {
                topViewIndex++;
                removeViewAt(0);
            } else {
                break;
            }
        }
        return distance;
    }

    private synchronized void verticalScroll(int distance) {
        totalDistanceScrolled += distance;
        int real_distance = Math.abs(distance + scrollRemainingDistance);

        if (distance < 0) {
            scrollRemainingDistance = - scrollUp(real_distance);
        } else {
            scrollRemainingDistance = scrollDown(real_distance);
        }
        scrollTo(0, totalDistanceScrolled);
    }

    private class ScrollingHandler implements Runnable {
        int mLastY;
        boolean mScrollDown;

        synchronized void fling(float v, float deltaY) {
            mScrollDown = deltaY < 0;
            if (scroller == null) {
                scroller = new Scroller(context);

            } else {
                scroller.forceFinished(true);
            }

            if (v < 0) {
                v = -v;
            }

            mLastY = 0;
            if (!mScrollDown && totalDistanceScrolled == 0) {
                return;
            }


            int distance;

            if (mScrollDown) {
                if (visibleHeight() > getHeight()) return;
                distance = leftToScrollDown();
                if (distance <= 0) {
                    return;
                }
            } else {
                distance = leftToScrollUp();
                if (distance <= 0) {
                    totalDistanceScrolled = 0;
                    return;
                }
            }

            scroller.setFinalY(Math.abs(distance));
            scroller.setFriction((float) 0.05);
            scroller.fling(0, mLastY, 0, (int) v, 0, 0, 0, distance);
            ViewGroup.this.post(this);
        }

        @Override
        public synchronized void run() {
            if (scroller == null || scroller.isFinished()) {
                return;
            }

            boolean again = scroller.computeScrollOffset();
            int curY = scroller.getCurrY();
            int distance = curY - mLastY;

            if (mScrollDown) {
                scrollingDown = !(scrollingUp = false);
                ViewGroup.this.verticalScroll(distance);
            } else {
                scrollingDown = !(scrollingUp = true);
                ViewGroup.this.verticalScroll(-distance);
            }
            mLastY = curY;

            if (!again) return;
            ViewGroup.this.post(this);
        }

    }

    private void stopScrolling() {
        if (scroller != null) {
            scroller.forceFinished(true);
            scroller = null;
        }
    }

    private int leftToScrollDown() {
        return getHeight() - totalDistanceScrolled - visibleHeight();
    }

    private int leftToScrollUp() {
        return totalDistanceScrolled;
    }

    public int scrolledDistance() {
        return totalDistanceScrolled;
    }

    public void setScrollMargin(int distance) {
        mScrollMargin = distance;
    }

    public void goToChild(int index){
        /*if(index < 0 || index > getChildCount()){
            return;
        }
        View view = getChildAt(index);
        if(view != null){
            verticalScroll((int) (view.getY() - totalDistanceScrolled));
        }*/
    }

    //**********************************************************************
    //              TOUCH EVENTS
    //**********************************************************************
    public boolean onTouch(View v, MotionEvent ev) {

        if (visibleViewCount == 0) return false;
        /*if (mInterceptor != null) {
            if (mInterceptor.onTouch(v, ev)) {
                return true;
            }
        }*/

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (scroller != null) {
                    scroller.forceFinished(true);
                }
                canSwipeDown = touchedViewPressed = !(mFinished = touchedViewReleased = isTouchIntercepted = hasMovedX = hasMovedY = longPressedHandlerCalled = mHasMoved = longPressed = swipeDown = false);
                velocityY = velocityX = totalDeltaX = totalDeltaY = 0;
                if (mLongClickTimer != null) {
                    mLongClickTimer.cancel();
                }

                if (tracker == null) {
                    tracker = VelocityTracker.obtain();
                }

                previousMoveX = downX = ev.getRawX();
                previousMoveY = downY = ev.getRawY();

                touchedChild = getViewFromCoordinates(getLeft() + (int) downX, (int) downY);

                if (touchedChild != null) {

                    if (mClickEffectsEnabled) {
                        touchedViewDrawable = touchedChild.getBackground();
                        //Effects.pressed(ctx, touchedChild);
                    }

                    final View view = touchedChild;
                    final Drawable drawable = touchedViewDrawable;

                    if (mLongClickEnabled) {
                        mLongClickTimer = new Timer();
                        mLongClickTimer.schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        if (itemLongClickedListener != null) {
                                            Handler h = ViewGroup.this.getHandler();
                                            if (h == null) return;
                                            h.post(() -> {
                                                if (!longPressedHandlerCalled && !mHasMoved && !mFinished) {
                                                    longPressed = true;
                                                    longPressedHandlerCalled = true;
                                                    if (mClickEffectsEnabled) {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                            view.setBackground(drawable);
                                                        } else {
                                                            view.setBackgroundDrawable(drawable);
                                                        }
                                                    }
                                                    itemLongClickedListener.onItemLongClicked(view, ViewGroup.this.indexOfChild(view) + topViewIndex);
                                                }
                                            });
                                        }
                                    }
                                },
                                LONG_PRESS_MIN_DURATION
                        );
                    }
                }
                return false;
            }


            case MotionEvent.ACTION_MOVE: {
                //if(isTouchIntercepted || mDrawing ) return true;
                if (tracker != null) {
                    tracker.addMovement(ev);
                    tracker.computeCurrentVelocity(250);
                    velocityY = tracker.getYVelocity();
                    velocityX = tracker.getXVelocity();
                }

                hasMovedY |= Math.abs(ev.getRawY() - downY) > slop;
                hasMovedX |= Math.abs(ev.getRawX() - downX) > slop;


                deltaY = (ev.getRawY() - previousMoveY);
                deltaX = (ev.getRawX() - previousMoveX);
                previousMoveY = ev.getRawY();
                previousMoveX = ev.getRawX();

                if (!mHasMoved && !hasMovedY && !hasMovedX) {
                    //move tolerance not exceeded yet
                    return false;
                }

                if (!mHasMoved) {
                    mHasMoved = true;
                }

                //if touched long click cancel move
                if (longPressed) {
                    return true;
                }

                //now has moved remove touch effect by setting the the old background of the touched child
                if (mClickEffectsEnabled && touchedViewPressed && !touchedViewReleased) {
                    touchedViewReleased = true;
                    touchedViewPressed = false;
                    if (touchedChild != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            touchedChild.setBackground(touchedViewDrawable);
                        } else {
                            touchedChild.setBackgroundDrawable(touchedViewDrawable);
                        }
                    }
                }


                totalDeltaX += deltaX;
                totalDeltaY += deltaY;

                if (hasMovedY) {
                    scrollingDown = !(scrollingUp = -deltaY < 0);
                    int distance = (int) deltaY;
                    if (distance < 0) {
                        if (visibleHeight() < getHeight() && !swipeDown) {
                            int scrollDown_left = leftToScrollDown();
                            distance = -distance;
                            if (distance > scrollDown_left) {
                                distance = scrollDown_left;
                            }
                            awakenScrollBars();
                            verticalScroll(distance);
                        } else if (swipeDown) {
                            //mSwipeDownListener.onSwipeDown(distance);
                        }

                    } else {
                        if (totalDistanceScrolled > 0 && !swipeDown) {
                            canSwipeDown = false;
                            distance = -distance;
                            if (distance + totalDistanceScrolled < 0) {
                                distance = -totalDistanceScrolled;
                            }
                            awakenScrollBars();
                            verticalScroll(distance);
                        } else {
                            /*swipeDown = mSwipeDownListener != null && canSwipeDown;
                            if(swipeDown){
                                mSwipeDownListener.onSwipeDown(distance);
                            }*/
                        }
                    }
                }
                return longPressed;
            }

            case MotionEvent.ACTION_CANCEL:
                mFinished = true;
                /*if(mSwipeDownListener != null) {
                    mSwipeDownListener.onCancelSwipeDown();
                }*/
                return true;

            case MotionEvent.ACTION_UP: {
                mFinished = true;
                if (longPressed) return true;

                if (touchedViewPressed && !touchedViewReleased && mClickEffectsEnabled) {
                    //Effects.released(ctx, touchedChild);
                }

                if (Math.abs(totalDeltaX) > Math.abs(totalDeltaY)) {
                    //if(mMoveListener != null) mMoveListener.onHorizontalFling(velocityX, deltaX);
                    return true;
                }/*else if(mSwipeDownListener != null && swipeDown){
                    mSwipeDownListener.onEndSwipeDown();
                    return true;
                }*/

                if (isTouchIntercepted) return true;
                scrollingDown = scrollingUp = false;

                if (!swipeDown && minFlingVelocity <= Math.abs(velocityY) && Math.abs(velocityY) <= maxFlingVelocity) {
                    awakenScrollBars();
                    new ScrollingHandler().fling(velocityY * 5, deltaY);
                    return true;
                }

                if (!mHasMoved && itemClickedListener != null && touchedChild != null) {
                    getHandler().postDelayed(() -> itemClickedListener.onItemClicked(touchedChild, ViewGroup.this.indexOfChild(touchedChild) + topViewIndex), 150);
                    return true;
                }
            }
        }
        return false;
    }

    private View getViewFromCoordinates(int x, int y) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);

            int location[] = new int[2];
            view.getLocationOnScreen(location);
            int viewX = location[0];
            int viewY = location[1];

            int height = view.getHeight();
            int width = view.getWidth();

            boolean insideBounds = (x > viewX && x < (viewX + width))
                    && (y > viewY && y < (viewY + height));
            if (insideBounds) {
                return view;
            }
        }
        return null;
    }

    public void setOnItemClickListener(OnItemClickedListener listener) {
        itemClickedListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickedListener listener) {
        itemLongClickedListener = listener;
    }
    @Override
    public void setOnClickListener(OnClickListener l) {
        this.mClickListener = l;
        super.setOnClickListener(l);
    }
}


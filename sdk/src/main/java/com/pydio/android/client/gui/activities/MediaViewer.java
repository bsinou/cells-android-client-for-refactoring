package com.pydio.android.client.gui.activities;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Connectivity;
import com.pydio.android.client.data.PreviewerData;
import com.pydio.android.client.data.PydioAgent;
import com.pydio.android.client.data.Resources;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.images.ImageBitmap;
import com.pydio.android.client.data.metrics.Measurement;
import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.model.WorkspaceNode;
import com.pydio.sdk.core.utils.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MediaViewer extends PydioNoBarActivity implements View.OnClickListener, View.OnTouchListener {

    int currentIndex;

    PydioAgent agent;
    PreviewerData data;
    ImageView imageView;

    float mDownX, mDownY, mDeltaX, mDeltaY, mVelocityY;
    VelocityTracker velocityTracker;

    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;

    boolean mMoveCancelled = true;
    ProgressBar mProgressBar;
    boolean mTapped;

    int SWIPE_MAX_DISTANCE;
    final int AUTO_SWIPE_DURATION = 300;

    int contentHeight, contentWidth;
    int mMaxHeight, mMaxWidth;
    int pageWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Application.previewerActivityData == null) {
            this.finish();
            return;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_media_viewer_layout);
        imageView = findViewById(R.id.preview);
        mProgressBar = findViewById(R.id.message_layout_progress_bar);

        imageView.setOnClickListener(this);
        imageView.setOnTouchListener(this);

        ViewConfiguration mViewConfig = ViewConfiguration.get(Application.context());
        mMinFlingVelocity = mViewConfig.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = mViewConfig.getScaledMaximumFlingVelocity();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mMaxWidth = size.x;
        mMaxHeight = size.y;

        String hStr = Application.getPreference(Application.PREF_PREVIOUS_SCREEN_HEIGHT);
        contentHeight = Integer.parseInt(hStr);
        contentWidth = Application.getContentWidth();

        SWIPE_MAX_DISTANCE = (int) getResources().getDimension(R.dimen.image_browse_max_swipe_distance);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (Resources.darkMainColor() == -1) {
                window.setStatusBarColor(getResources().getColor(R.color.main_color_dark));
            } else {
                window.setStatusBarColor(Resources.darkMainColor());
            }
        }

        currentIndex = Application.previewerActivityData.getIndex();

        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }
    @Override
    protected void onStart() {
        super.onStart();
        hideFAB();
        Node node = Application.previewerActivityData.getNodes().get(currentIndex);
        load(node, null);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    String getWorkspaceSlug(Node node) {
        if (node.type() == Node.TYPE_WORKSPACE) {
            return node.id();
        } else if (node.type() == Node.TYPE_REMOTE_NODE){
            String slug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
            if (slug != null && !"".equals(slug)) {
                return slug;
            }

            String workspaceUuid = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_UUID);
            if (workspaceUuid == null) {
                return null;
            }

            WorkspaceNode wn = Application.previewerActivityData.getSession().server.findWorkspaceById(workspaceUuid);
            if (wn != null) {
                return wn.id();
            }
            return null;
        } else {
            return null;
        }
    }

    private void load(Node node, final Runnable r) {
        if(agent == null){
            Session s = Application.previewerActivityData.getSession();
            agent = new PydioAgent(s);
        }

        String workspaceSlug = getWorkspaceSlug(node);
        String path = node.path();

        mProgressBar.setVisibility(View.VISIBLE);
        String downloadedPath = agent.session.downloadPath(workspaceSlug, path);
        if(new File(downloadedPath).exists()) {
            Background.go(() -> {
                float[] dims = ImageBitmap.dimensions(downloadedPath);
                float ratio = dims[0] / dims[1];
                Bitmap bm = ImageBitmap.loadBitmap(downloadedPath, (int)dims[0], (int)dims[1], ((int)ratio)==1);
                getHandler().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    imageView.setImageBitmap(bm);
                    if (r != null) {
                        r.run();
                    }
                });
            });
            return;
        }

        Connectivity con = Connectivity.get(this);

        if(!con.icConnected()){
            showMessage(R.string.no_active_connection);
            return;
        }

        if(con.isCellular() && !con.isCellularDownloadAllowed()){
            showMessage(R.string.download_on_cellular_not_allow);
            return;
        }

        agent.downloadURL(node, (url, error) -> {
            if(error != null) {
                showMessage(getString(R.string.failed_to_get_download_url));
                getHandler().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    imageView.setImageResource(R.drawable.broken_image);
                    if (r != null) {
                        r.run();
                    }
                });
                return;
            }

            try {
                HttpURLConnection connection;
                if (agent.session.server.isSSLUnverified()) {
                    HttpsURLConnection c = (HttpsURLConnection) new URL(url).openConnection();
                    c.setSSLSocketFactory(agent.session.server.getSslContext().getSocketFactory());
                    c.setHostnameVerifier(agent.session.server.getHostnameVerifier());
                    connection = c;
                } else {
                    URL imageURL = new URL(url);
                    connection = (HttpURLConnection) imageURL.openConnection();
                }

                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();

                OutputStream out = new FileOutputStream(downloadedPath);
                io.pipeRead(input, out);
                io.close(out);
                io.close(input);

                float[] dims = ImageBitmap.dimensions(downloadedPath);
                float ratio = dims[0] / dims[1];

                float width = Math.max(Measurement.getScreen_width(this), dims[0]);
                float height = width * ratio;
                Bitmap bm = ImageBitmap.loadBitmap(downloadedPath, (int)width, (int)height, ((int)ratio)==1);
                getHandler().post(() -> imageView.setImageBitmap(bm));

            } catch (Exception e) {
                showMessage(getString(R.string.failed_to_get_download_url));
            }
            getHandler().post(() -> {
                mProgressBar.setVisibility(View.GONE);
                if (r != null) {
                    r.run();
                }
            });
        });
    }

    private void next() {
        mProgressBar.setVisibility(View.VISIBLE);
        int index = (this.currentIndex + 1) % Application.previewerActivityData.getNodes().size();
        Node node = Application.previewerActivityData.getNodes().get(index);
        this.currentIndex = index;
        imageView.setImageResource(0);

        load(node, () -> {
            Animation anim = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    super.applyTransformation(interpolatedTime, t);
                    imageView.setAlpha(interpolatedTime);
                    imageView.setX(SWIPE_MAX_DISTANCE - (SWIPE_MAX_DISTANCE * interpolatedTime));
                    imageView.requestLayout();
                }
            };
            anim.setDuration(AUTO_SWIPE_DURATION);
            anim.setFillAfter(true);
            imageView.startAnimation(anim);
        });
    }

    private void previous() {
        mProgressBar.setVisibility(View.VISIBLE);
        int index = this.currentIndex - 1;
        if (index < 0) index = Application.previewerActivityData.getNodes().size() - 1;
        Node node = Application.previewerActivityData.getNodes().get(index);
        this.currentIndex = index;
        imageView.setImageResource(0);
        load(node, () -> {
            Animation anim = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    super.applyTransformation(interpolatedTime, t);
                    imageView.setAlpha(interpolatedTime);
                    imageView.setX((SWIPE_MAX_DISTANCE * interpolatedTime) - SWIPE_MAX_DISTANCE);
                    imageView.requestLayout();
                }
            };
            anim.setDuration(AUTO_SWIPE_DURATION);
            anim.setFillAfter(true);
            imageView.startAnimation(anim);
        });

    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        /*if(id == mPreview.getId()){
            if(mBrowsingLayout.getVisibility() == View.VISIBLE){
                mBrowsingLayout.setVisibility(View.GONE);
            }else if(mBrowsingLayout.getVisibility() == View.GONE){
                mBrowsingLayout.setVisibility(View.VISIBLE);
            }
        }else if(id == mBrowseLeftButton.getId()){
            previous();
        }else if(id == mBrowseRightButton.getId()){
            next();
        }*/
    }
    @Override
    public boolean onTouch(View view, MotionEvent e) {
        final int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mTapped = true;
                mVelocityY = 0;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    try {
                        velocityTracker.clear();
                    } catch (IllegalStateException ex) {
                    }
                }
                mDeltaX = mDeltaY = 0;
                mDownX = e.getRawX();
                mDownY = e.getRawY();
                mMoveCancelled = false;
                imageView.clearAnimation();
            }

            case MotionEvent.ACTION_MOVE: {

                if (mMoveCancelled) {
                    return true;
                }
                if (velocityTracker != null) {
                    velocityTracker.addMovement(e);
                    velocityTracker.computeCurrentVelocity(1500);
                    mVelocityY = velocityTracker.getYVelocity();
                }
                mProgressBar.setVisibility(View.VISIBLE);

                mDeltaY = (e.getRawY() - mDownY);
                mDeltaX = (e.getRawX() - mDownX);
                float level = Math.min(Math.abs(mDeltaX), SWIPE_MAX_DISTANCE) / SWIPE_MAX_DISTANCE;
                imageView.setAlpha(1 - level);
                imageView.setX(mDeltaX);
                imageView.requestLayout();
                return false;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                mProgressBar.setVisibility(View.GONE);
                if (Math.abs(mDeltaX) > SWIPE_MAX_DISTANCE) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mMoveCancelled = true;
                    swipe(mDeltaX < 0);
                    return mMoveCancelled;
                } else {
                    final float x = imageView.getX();
                    final float curAlpha = imageView.getAlpha();
                    final float alpha_left = 1 - imageView.getAlpha();
                    Animation anim = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            super.applyTransformation(interpolatedTime, t);
                            imageView.setAlpha(interpolatedTime);
                            imageView.setAlpha(curAlpha + (interpolatedTime * alpha_left));
                            imageView.setX(x - (x * interpolatedTime));
                        }
                    };
                    anim.setDuration(200);
                    imageView.startAnimation(anim);
                }

                if (mMinFlingVelocity <= Math.abs(mVelocityY) && Math.abs(mVelocityY) <= mMaxFlingVelocity) {
                    flingToClose();
                    return true;
                }
            }
        }
        return view.performClick();
    }

    public void swipe(boolean next) {
        if (next) {
            next();
        } else {
            previous();
        }
    }

    public void flingToClose() {
        if (mDeltaY > 0) {
            //Toast.makeText(this, "fling to close : " + deltaY, Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(this, "fling to close : " + deltaY, Toast.LENGTH_SHORT).show();
        }
    }

}

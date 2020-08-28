package com.pydio.android.client.data.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.opengl.GLES10;
import android.os.Debug;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.auth.AppCredentials;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.services.Cache;
import com.pydio.android.client.utils.Background;
import com.pydio.android.client.utils.Task;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.ClientFactory;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.Node;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PreviewLoader {

    private static final String lock = "";

    private static int OPEN_GL_MAX_TEXTURE_SIZE = 0;
    private static Map<ImageView, Request> requests = new HashMap<>();
    private static Task tracker;

    private static void storeBitmap(Bitmap b, String path) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            //Log.e("unspecific", e.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                //Log.e("IO", e.getMessage());
            }
        }
    }

    private static Bitmap loadBitmap(String path, int w, int h, boolean square) {
        if (getOpenGlMaxTextureSize() > 0) {
            w = h = Math.min(Math.min(w, h), getOpenGlMaxTextureSize());
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        if (!checkBitmapFitsInMemory(options.outWidth, options.outHeight, Math.max(options.inTargetDensity, Math.max(options.inScreenDensity, options.inDensity)))) {
            if (w == 0 || h == 0) return null;
            return loadBitmap(path, w / 2, h / 2, square);
        }

        options.inSampleSize = calculateSampleSize(options, w, h);
        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        if (!square) {
            return bm;
        }

        int dim = Math.max(w, h);
        return ThumbnailUtils.extractThumbnail(bm, dim, dim);
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;

        if (width > reqWidth || height > reqHeight) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public static int[] dimensions(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int width = options.outWidth;
        int height = options.outHeight;
        return new int[]{width, height};
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        if (newWidth >= width || newHeight >= height) {
            return bm;
        }

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private static boolean checkBitmapFitsInMemory(long bmpwidth, long bmpheight, int bmpdensity) {
        long reqsize = bmpwidth * bmpheight * bmpdensity;
        long allocNativeHeap = Debug.getNativeHeapAllocatedSize();

        final long heapPad = (long) Math.max(4 * 1024 * 1024, Runtime.getRuntime().maxMemory() * 0.1);
        return (reqsize + allocNativeHeap + heapPad) < Runtime.getRuntime().maxMemory();
    }

    private static int getOpenGlMaxTextureSize() {
        if (OPEN_GL_MAX_TEXTURE_SIZE > 0) return OPEN_GL_MAX_TEXTURE_SIZE;
        int[] maxTextureSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        return OPEN_GL_MAX_TEXTURE_SIZE = maxTextureSize[0];
    }

    public static void loadPreview(ImageView v, Request r, boolean enqueue) {
        synchronized (lock) {
            requests.put(v, r);
        }

        if (tracker == null || tracker.taskDone()) {
            next();
        }
    }

    private static void next() {
        ImageView view = null;
        Request request = null;
        synchronized (lock) {
            Iterator it = requests.keySet().iterator();
            if (it.hasNext()) {
                view = (ImageView) it.next();
                request = requests.get(view);
                it.remove();
            }
        }

        if (view != null && request != null) {
            load(view, request);
        }
    }

    private static void load(View v, Request r) {

        final Bitmap[] bitmap = new Bitmap[]{null};
        Runnable setImage = () -> {
            Bitmap finalBitmap = bitmap[0];
            v.setScaleX(1);
            v.setScaleY(1);
            ((ImageView) v).setImageBitmap(finalBitmap);
        };

        long mtime = Long.parseLong(r.node.getProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME));
        final Thumb t = Cache.getThumbnail(r.workspace, r.node.path(), r.height);

        if (t != null && t.bitmap != null) {
            bitmap[0] = t.bitmap;
            Background.go(() -> r.handler.post(setImage));
            if (t.editTime <= mtime) {
                return;
            }
        }

        if (r.session != null) {
            Client client = ClientFactory.get().Client(r.session.server);
            client.setTokenStore(Database::saveToken);
            client.setTokenProvider(Database::getToken);
            AppCredentials credentials = new AppCredentials(r.session.server.url());
            credentials.setLogin(r.session.user);
            client.setCredentials(credentials);

            try {
                String thumbsURLs = r.node.getProperty(Pydio.NODE_PROPERTY_IMAGE_THUMB_PATHS);
                if (thumbsURLs != null && !"".equals(thumbsURLs)) {
                    JSONObject json = new JSONObject(thumbsURLs);
                    for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                        String k = it.next();
                        if (k.equals(String.valueOf(r.width)) || k.equals(String.valueOf(r.height))) {
                            String url = json.getString(k);
                            String ws = r.workspace;
                            InputStream in = client.previewData(ws, url, r.width);
                            bitmap[0] = BitmapFactory.decodeStream(in);
                            break;
                        }
                    }
                } else {

                    String url;
                    if (client.getServerNode().versionName().contains("cells")){
                        url = "/" + r.node.id() + "-256.jpg";
                    } else {
                        url = r.node.path();
                    }
                    InputStream in = client.previewData(r.workspace, url, r.width);
                    bitmap[0] = BitmapFactory.decodeStream(in);
                }

            } catch (SDKException e) {
                e.printStackTrace();
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            bitmap[0] = loadBitmap(r.node.path(), r.width, r.height, r.square);
        }

        if (bitmap[0] != null) {
            String path = r.session.cacheFolderPath() + File.separator + UUID.randomUUID().toString() + ".jpeg";
            storeBitmap(bitmap[0], path);
            Cache.saveThumb(r.workspace, r.node.path(), path, mtime, r.width);
            r.handler.post(setImage);
        }
    }

    public static class Request {
        public Node node;
        public Session session;
        public String workspace;
        public Handler handler;
        public int width;
        public int height;
        public boolean square;
    }
}

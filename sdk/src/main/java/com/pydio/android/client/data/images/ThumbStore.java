package com.pydio.android.client.data.images;

import android.graphics.Bitmap;

import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Connectivity;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.auth.AppCredentials;
import com.pydio.android.client.data.callback.Completion;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.services.Cache;
import com.pydio.android.client.utils.Background;
import com.pydio.android.client.utils.Threading;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.utils.io;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ThumbStore implements ThumbLoader {

    private final Object lock = 0;
    private boolean running = false;

    private Session session;

    private List<Request> requests;

    public ThumbStore(Session session) {
        this.session = session;
        requests = new ArrayList<>();
    }

    public void stop(){
        synchronized (lock){
            running = false;
            requests.clear();
        }
    }

    public void start(){
        synchronized (lock){
            if(running){
                return;
            }
            running = true;
        }
        Background.go(this::consume);
    }
    @Override
    public void loadThumb(Node node, int dimen, Completion<Bitmap> completion) {
        Request r = new Request();
        r.node = node;
        r.width = dimen;
        r.height = dimen;
        r.completion = completion;

        synchronized (lock){
            if(running) {
                requests.add(r);
            }
        }
    }

    public static class Request {
        public Node node;
        public int width;
        public int height;
        public Completion<Bitmap> completion;
    }

    private void consume() {
        for(;;){
            synchronized (lock){
                if(!running){
                    break;
                }
            }

            Iterator it;
            Request request = null;
            synchronized (lock) {
                it = requests.iterator();
                if (it.hasNext()) {
                    request = (Request) it.next();
                    it.remove();
                }
            }

            if(request != null) {
                load(request);
            } else {
                Threading.sleep(400);
            }
        }
    }

    private void load(Request r) {
        final String ws = r.node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);

        String localWs = session.id() + "." + ws;
        final Bitmap[] bitmap = new Bitmap[]{null};
        Runnable setImage = () -> {
            Bitmap finalBitmap = bitmap[0];
            r.completion.onComplete(finalBitmap);
        };

        long mtime = 256;

        String mtimeProp = r.node.getProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME);
        if (mtimeProp != null) {
            mtime = Long.parseLong(mtimeProp);
        }

        final Thumb t = Cache.getThumbnail(localWs, r.node.path(), r.height);
        if (t != null && t.bitmap != null) {
            bitmap[0] = t.bitmap;
            setImage.run();
            if(t.editTime <= mtime){
                return;
            }
        }

        Connectivity con = Connectivity.get(Application.context());
        if (!con.icConnected()){
            return;
        }

        if(con.isCellular() && !con.isCellularImagePreviewDownloadAllowed()){
            return;
        }

        String thumbPath = this.session.cacheFolderPath() + File.separator + UUID.randomUUID().toString() + ".jpeg";

        if (this.session != null){
            Client client = Client.get(this.session.server);
            client.setTokenStore(Database::saveToken);
            client.setTokenProvider(Database::getToken);
            AppCredentials credentials = new AppCredentials(this.session.server.url());
            credentials.setLogin(this.session.user);
            client.setCredentials(credentials);

            try {
                String thumbsURLs = r.node.getProperty(Pydio.NODE_PROPERTY_IMAGE_THUMB_PATHS);
                if(thumbsURLs != null && !"".equals(thumbsURLs)) {
                    JSONObject json = new JSONObject(thumbsURLs);
                    for (Iterator<String> it = json.keys(); it.hasNext();) {
                        String k = it.next();
                        int size = Integer.valueOf(k);
                        if(size > 0 && size >= r.width || size  >= r.height){
                            String url = json.getString(k);

                            InputStream in = client.previewData(ws, url, r.width);
                            byte[] buffer = new byte[16384];
                            OutputStream out = new FileOutputStream(thumbPath);
                            for(;;){
                                int n = in.read(buffer);
                                if(n == -1) break;
                                out.write(buffer, 0, n);
                            }
                            io.close(in);
                            io.close(out);
                            Bitmap bmp = ImageBitmap.loadBitmap(thumbPath, r.width, r.height, r.width == r.height);
                            ImageBitmap.storeBitmap(bmp, thumbPath);
                            bitmap[0] = bmp;
                            break;
                        }
                    }

                } else {
                    InputStream in = client.previewData(ws, r.node.path(), r.width);
                    byte[] buffer = new byte[16384];
                    OutputStream out = new FileOutputStream(thumbPath);
                    for(;;){
                        int n = in.read(buffer);
                        if(n == -1) break;
                        out.write(buffer, 0, n);
                    }

                    io.close(in);
                    io.close(out);
                    Bitmap bmp = ImageBitmap.loadBitmap(thumbPath, r.width, r.height, r.width == r.height);
                    ImageBitmap.storeBitmap(bmp, thumbPath);
                    bitmap[0] = bmp;
                }

                if(bitmap[0] != null){
                    ImageBitmap.storeBitmap(bitmap[0], thumbPath);
                    Cache.saveThumb(localWs, r.node.path(), thumbPath, mtime, r.width);
                    setImage.run();
                }
            } catch (SDKException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        } else {
            bitmap[0] = ImageBitmap.loadBitmap(r.node.path(), r.width, r.height, true);
        }
    }

    public ThumbLoader getDelegate(){
        return this;
    }
}

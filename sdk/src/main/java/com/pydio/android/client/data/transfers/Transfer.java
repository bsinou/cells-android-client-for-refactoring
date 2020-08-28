package com.pydio.android.client.data.transfers;

import android.database.Cursor;

import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.auth.AppCredentials;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.utils.Task;
import com.pydio.android.client.utils.Threading;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.ClientFactory;
import com.pydio.sdk.core.common.callback.Completion;
import com.pydio.sdk.core.common.callback.MessageCompletion;
import com.pydio.sdk.core.common.callback.ProgressListener;
import com.pydio.sdk.core.common.callback.TransferProgressListener;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Message;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transfer {

    private Transfer() {
    }

    private static final Object globalLock = new Object();
    private static boolean consuming = false;
    private static boolean queueStopRequested = false;

    public static final int download = 1;
    public static final int upload = 2;

    public static final int done = 0;
    public static final int enqueued = 1;
    public static final int waiting = 2;
    public static final int running = 3;
    public static final int stopped = 4;
    public static final int failed = 5;

    public static final int NodeType = 50;

    private static Map<String, Listener> listeners = new HashMap<>();


    private final Object lock = new Object();

    private long id;
    private int type;
    private int status;
    private String session;
    private String workspace;
    private String local;
    private String remote;
    private long size;

    private long progress;

    private List<ProgressListener> progressListeners;
    private boolean stopRequested = false;
    private boolean mustRunInQueue = true;


    //**********************************************
    //              CREATE - MANAGEMENT
    //**********************************************
    public static Transfer parse(Cursor c) {
        Transfer t = new Transfer();
        t.id = c.getLong(0);
        t.session = c.getString(1);
        t.workspace = c.getString(2);
        t.type = c.getInt(3);
        t.remote = c.getString(4);
        t.local = c.getString(5);
        t.size = c.getLong(6);
        t.status = c.getInt(7);
        t.mustRunInQueue = true;
        return t;
    }

    public static Transfer newDownload(Session session, String workspace, FileNode node) {
        Transfer t = new Transfer();
        t.status = waiting;
        t.session = session.id();
        t.workspace = workspace;
        t.type = download;
        t.remote = node.path();
        t.local = session.downloadPath(workspace, node.path()).replace("//", "/");
        t.size = node.size();
        t.mustRunInQueue = false;
        return t;
    }

    public static Transfer newUpload(Session session, String workspace, String folder, File file) {
        Transfer t = new Transfer();
        t.type = upload;
        t.status = waiting;
        t.session = session.id();
        t.workspace = workspace;
        t.remote = folder;
        t.local = file.getPath();
        t.size = file.length();
        t.mustRunInQueue = false;
        return t;
    }

    public static Transfer newUpload(Session session, String workspace, String folder, URI uri, long size) {
        Transfer t = new Transfer();
        t.type = upload;
        t.status = waiting;
        t.session = session.id();
        t.workspace = workspace;
        t.remote = folder;
        t.local = uri.toString();
        t.size = -1;
        t.mustRunInQueue = false;
        return t;
    }

    public static void enqueueDownload(Session session, String workspace, FileNode node) {
        Transfer t = newDownload(session, workspace, node);
        Database.addTransfer(t);
    }

    public static void enqueueUpload(Session session, String workspace, String folder, File file) {
        Transfer t = newUpload(session, workspace, folder, file);
        Database.addTransfer(t);
    }

    public static void enqueueDownloads(Session session, String workspace, List<FileNode> nodes) {
        List<Transfer> transfers = new ArrayList<>();
        for (FileNode node : nodes) {
            Transfer t = newDownload(session, workspace, node);
            transfers.add(t);
        }
        Database.addTransfers(transfers);
    }

    public static void enqueueUploads(Session session, String workspace, String folder, List<File> files) {
        List<Transfer> transfers = new ArrayList<>();
        for (File file : files) {
            Transfer t = newUpload(session, workspace, folder, file);
            transfers.add(t);
        }
        Database.addTransfers(transfers);
    }

    public static void stopConsuming(){
        queueStopRequested = true;
    }

    public static void processQueue() {
        consumeQueue();
    }

    private static void consumeQueue() {
        queueStopRequested = false;

        synchronized (globalLock){
            if(consuming){
                return;
            }
        }
        consuming = true;


        for(;!queueStopRequested;) {
            try{
                final Transfer t = Database.dequeueTransfer();
                if (t == null) {
                    Threading.sleep(2000);
                    continue;
                }

                final String dir, name;
                if(t.type == upload){
                    dir = t.remote;
                    name = new File(t.local).getName();
                } else {
                    File f = new File(t.remote);
                    dir = f.getParent();
                    name = f.getName();
                }

                notifyNew(t.session, t.workspace, dir, name, t.type, t.size);
                t.mustRunInQueue = false;
                t.run((progress) -> notifyProgress(t.session, t.workspace, dir, name, t.type, progress, t.size), (msg, error) -> {
                    if(error != null) {
                        notifyError(t.session, t.workspace, dir, name, t.type, error);
                    } else {
                        notifyFinish(t.session, t.workspace, dir, name, t.type, msg);
                    }
                    synchronized (globalLock) {consuming = false;}
                    Database.deleteTransfer(t.id);
                });
                break;
            }catch (Exception ignore){
                ignore.printStackTrace();
            }
        }
        synchronized (globalLock){
            consuming = false;
        }
    }

    //**********************************************
    //              Events
    //**********************************************
    public static void addTransferListener(String id, Listener listener) {
        synchronized (globalLock) {
            if (listeners == null) {
                listeners = new HashMap<>();
            }
            listeners.put(id, listener);
        }
    }

    public static void cancel(String session, String ws, String directory, String name) {

    }

    private static void notifyNew(String session, String ws, String dir, String name, int type, long size) {
        synchronized (globalLock) {
            for (Map.Entry e : listeners.entrySet()) {
                Listener l = (Listener) e.getValue();
                l.onNew(session, ws, dir, name, type, size);
            }
        }
    }

    private static void notifyProgress(String session, String ws, String dir, String name, int type, long progress, long size) {
        synchronized (globalLock) {
            for (Map.Entry e : listeners.entrySet()) {
                Listener l = (Listener) e.getValue();
                l.onProgress(session, ws, dir, name, type, progress, size);
            }
        }
    }

    private static void notifyError(String session, String ws, String dir, String name, int type, Error error) {
        synchronized (globalLock) {
            for (Map.Entry e : listeners.entrySet()) {
                Listener l = (Listener) e.getValue();
                l.onError(session, ws, dir, name, type, error);
            }
        }
    }

    private static void notifyFinish(String session, String ws, String dir, String name, int type, Message msg) {
        synchronized (globalLock) {
            for (Map.Entry e : listeners.entrySet()) {
                Listener l = (Listener) e.getValue();
                l.onFinish(session, ws, dir, name, type, msg);
            }
        }
    }

    public interface ManagerListener {
        void onStatusUpdated(int old, Transfer t);
    }

    public class Controller {
        private Task t;

        private Controller(Task t) {
            this.t = t;
        }

        public void stop() {
            t.cancel();
        }

        public Controller then(Completion c) {
            return this;
        }
    }

    //**********************************************
    //              GETTER / SETTER
    //**********************************************
    public int type() {
        return type;
    }

    public long getProgress() {
        synchronized (lock) {
            return progress;
        }
    }

    public long getSize() {
        return size;
    }

    public int status() {
        synchronized (lock) {
            return status;
        }
    }

    public int getType() {
        return type;
    }

    public int getStatus() {
        return status;
    }

    public String getSession() {
        return session;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getLocal() {
        return local;
    }

    public String getRemote() {
        return remote;
    }

    public void addProgressListener(ProgressListener listener) {
        if (progressListeners == null) {
            progressListeners = new ArrayList<>();
        }
        progressListeners.add(listener);
    }

    public TransferProgressListener getProgressListener() {
        return (progress -> {
            if (progressListeners != null) {
                for (ProgressListener listener : progressListeners) {
                    synchronized (lock) {
                        this.progress = progress;
                    }

                    if (listener != null) {
                        listener.onProgress(progress);
                    }
                }
            }
            synchronized (lock) {
                return stopRequested;
            }
        });
    }

    //***********************************************************
    //              RUN
    //***********************************************************
    public void run(ProgressListener listener, MessageCompletion completion) {
        if (mustRunInQueue) {
            return;
        }

        addProgressListener(listener);

        if (type == download) {
            runDownload((error) -> completion.onComplete(null, error));
        } else if (type == upload) {
            runUpload(completion);
        }
    }

    private void runDownload(Completion completion) {
        Session session = Application.findSession(this.session);

        Client client = ClientFactory.get().Client(session.server);
        client.setTokenProvider(Database::getToken);
        client.setTokenStore(Database::saveToken);
        AppCredentials credentials = new AppCredentials(session.server.url());
        credentials.setLogin(session.user);
        client.setCredentials(credentials);

        try {
            client.download(workspace, remote, new File(local), getProgressListener());
            completion.onComplete(null);
        } catch (SDKException e) {
            completion.onComplete(Error.fromException(e));
        }
    }

    private void runUpload(MessageCompletion completion) {
        Session session = Application.findSession(this.session);

        Client client = ClientFactory.get().Client(session.server);
        client.setTokenProvider(Database::getToken);
        client.setTokenStore(Database::saveToken);
        AppCredentials credentials = new AppCredentials(session.server.url());
        credentials.setLogin(session.user);
        client.setCredentials(credentials);

        try {
            File file = new File(local);
            Message msg = client.upload(file, workspace, remote,  file.getName(), true, getProgressListener());
            completion.onComplete(msg, null);
        } catch (SDKException e) {
            completion.onComplete(null, Error.fromException(e));
        }
    }

    //**********************************************
    //              CONTROL
    //**********************************************
    public void stop() {
        synchronized (lock) {
            stopRequested = true;
        }
    }
}

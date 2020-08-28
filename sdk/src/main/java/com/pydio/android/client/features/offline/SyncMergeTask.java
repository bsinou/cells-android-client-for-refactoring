package com.pydio.android.client.features.offline;

import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.auth.AppCredentials;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.utils.Threading;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.Pydio8;
import com.pydio.sdk.core.ClientFactory;
import com.pydio.sdk.core.PydioCells;
import com.pydio.sdk.core.model.Change;
import com.pydio.sdk.sync.Error;
import com.pydio.sdk.sync.FilePersistedMergeState;
import com.pydio.sdk.sync.MergeActivityListener;
import com.pydio.sdk.sync.Merger;
import com.pydio.sdk.sync.Watch;
import com.pydio.sdk.sync.changes.ChangeStore;
import com.pydio.sdk.sync.changes.FileChangeStore;
import com.pydio.sdk.sync.changes.FileWatchStore;
import com.pydio.sdk.sync.fs.CellsFs;
import com.pydio.sdk.sync.fs.Fs;
import com.pydio.sdk.sync.fs.LocalFs;
import com.pydio.sdk.sync.fs.Pydio8Fs;

import java.io.File;
import java.io.IOException;

public class SyncMergeTask implements MergeActivityListener{
    private Session session;
    private String workspace;

    private FileWatchStore watchStore;
    private FilePersistedMergeState mergeState;

    private Fs localFs;

    private boolean stopped;
    private Thread thread;
    private MergeActivityListener mergeActivityListener;
    private Merger merger;

    public SyncMergeTask(Session session, String workspace) throws IOException {
        this.session = session;
        this.workspace = workspace;

        String offlineFolder = session.offlineTaskFolder(this.workspace);

        File watches = new File(offlineFolder, "watchStore.json");
        File changes = new File(offlineFolder, "changes.json");
        File mergeState = new File(offlineFolder, "state.json");

        ChangeStore changeStore = new FileChangeStore(changes.getPath());
        this.watchStore = new FileWatchStore(watches.getPath());
        this.mergeState = new FilePersistedMergeState(mergeState.getPath());

        this.localFs = new LocalFs("local", session.workspacePath(workspace), this.watchStore.getWatches());
        this.stopped = false;

        Client client = ClientFactory.get().Client(this.session.server);
        client.setTokenStore(Database::saveToken);
        client.setTokenProvider(Database::getToken);
        AppCredentials credentials = new AppCredentials(this.session.server.url());
        credentials.setLogin(this.session.user);
        client.setCredentials(credentials);

        Fs remoteFs;
        if (session.server.versionName().toLowerCase().contains("cells")) {
            //FIXME initialize with the stateManager
            remoteFs = new CellsFs("cells-" + session.id(), (PydioCells) client, this.workspace, null);
        } else {
            remoteFs = new Pydio8Fs("pydio8-" + session.id(), (Pydio8) client, this.workspace);
        }

        merger = new Merger(this.mergeState, changeStore, this.localFs, remoteFs);
    }

    public void watch(String path) {
        watchStore.addWatch(path);
    }

    public void unwatch(String path) {
        watchStore.deleteWatch(path);
    }

    public boolean isWatched(String path) {
        return watchStore.isWatched(path);
    }

    public boolean isUnderAWatched(String path) {
        return watchStore.isUnderWatched(path);
    }

    public int countChanges() {
        if(merger == null) {
            return -1;
        }
        int count = merger.countChanges();
        Error error = merger.fetchChanges();
        if (error != null) {
            return -1;
        }
        count += merger.countChanges();

        return count;
    }

    public Session getSession() {
        return session;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void start(MergeActivityListener mergeActivityListener) {
        this.mergeActivityListener = mergeActivityListener;
        thread = new Thread(this::run);
        thread.start();
    }

    public void saveState(){
        try {
            this.mergeState.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() {
        stopped = false;
        while (!stopped) {
            try {
                // Merge Fs here
                this.mergeState.load();
                for (String path : watchStore.getWatches()) {
                    localFs.addWatch(path);
                }
                this.mergeState.save();

                Error error = merger.merge(this);
                if (error == null) {
                    this.mergeState.save();
                }
                Threading.sleep(2000);
            } catch (Exception ignored) {
                Threading.sleep(5000);
            }
        }
    }

    public void stop() {
        stopped = true;
        if (thread != null) {
            thread.interrupt();
            thread = null;
            merger = null;
        }
    }

    @Override
    public void onActionCompleted(Change c) {
        if (c.getType().equals(Change.TYPE_DELETE)) {
            this.watchStore.deleteWatch(c.getSource());
        }

        if (this.mergeActivityListener != null) {
            this.mergeActivityListener.onActionCompleted(c);
        }
    }

    @Override
    public void onActionFailed(Error error, Change c) {
        try {
            this.mergeState.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.mergeActivityListener != null) {
            this.mergeActivityListener.onActionFailed(error, c);
        }
    }

    @Override
    public void onChangesCount(Watch w, int count) {

        try {
            this.mergeState.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.mergeActivityListener != null) {
            this.mergeActivityListener.onChangesCount(w, count);
        }
    }
}

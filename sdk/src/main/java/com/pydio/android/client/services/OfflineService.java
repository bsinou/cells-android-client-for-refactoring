package com.pydio.android.client.services;

import com.pydio.android.client.data.Session;
import com.pydio.android.client.features.offline.EventsListener;
import com.pydio.android.client.features.offline.SyncMergeTask;
import com.pydio.android.client.utils.Threading;
import com.pydio.sdk.core.model.Change;
import com.pydio.sdk.core.model.WorkspaceNode;
import com.pydio.sdk.sync.Error;
import com.pydio.sdk.sync.MergeActivityListener;
import com.pydio.sdk.sync.Watch;

import java.io.IOException;
import java.util.List;

public class OfflineService implements MergeActivityListener {

    public static final int INVALID_SYNC_STATE = -1;
    public static final int UNWATCHED = 0;
    public static final int WATCHED = 1;
    public static final int UNDER_A_WATCHED = 2;

    private static final Object lock = 0;
    private static OfflineService instance;
    private SyncMergeTask mergeTask;
    private volatile boolean pollStopped;

    private Thread changePollThread;
    private EventsListener eventsListener;
    private String workspaceId;
    private Session session;
    private List<String> polledWorkspaceIds;

    public OfflineService(Session session) {
        this.session = session;
        this.pollStopped = false;
    }

    public void watch(String path) {
        if (mergeTask == null) {
            return;
        }
        mergeTask.watch(path);
    }

    public void unWatch(String path) {
        if (mergeTask == null) {
            return;
        }
        mergeTask.unwatch(path);
    }

    public void start(List<WorkspaceNode> workspaceNodes) {

        if (changePollThread != null) {
            if (!changePollThread.isInterrupted()) {
                changePollThread.interrupt();
            }
        }

        changePollThread = new Thread(() -> {
            pollStopped = false;
            while (!pollStopped) {
                if(session != null) {
                    final String sessionID = session.id();
                    if(eventsListener != null) {
                        eventsListener.onNewChanges(sessionID, workspaceId, 0);
                    }
                    for (WorkspaceNode workspaceNode: workspaceNodes) {
                        if (workspaceNode.syncable() && !workspaceNode.id().equals(workspaceId)) {
                            try {
                                SyncMergeTask syncMergeTask = new SyncMergeTask(session, workspaceNode.id());
                                int count = syncMergeTask.countChanges();
                                if(eventsListener != null && count > 0) {
                                    eventsListener.onNewChanges(sessionID, workspaceNode.id(), count);
                                }
                                syncMergeTask.saveState();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                Threading.sleep(5000);
            }
        });
        changePollThread.start();
    }

    public void stop() {
        if (changePollThread != null) {
            if (!changePollThread.isInterrupted()) {
                changePollThread.interrupt();
            }
            pollStopped = true;
            changePollThread = null;
        }

        if (mergeTask != null) {
            mergeTask.stop();
            mergeTask = null;
        }
    }

    private boolean isUnderAWatched(String path) {
        if(mergeTask == null){
            return false;
        }
        return mergeTask.isUnderAWatched(path);
    }

    private boolean isWatched(String path){
        if(mergeTask == null){
            return false;
        }
        return mergeTask.isWatched(path);
    }
    @Override
    public void onActionCompleted(Change c) {
        if (this.eventsListener == null) {
            return;
        }
        this.eventsListener.onChangeProcessed(session.id(), workspaceId, c.getNode().getPath());
    }
    @Override
    public void onActionFailed(Error error, Change c) {
        if (this.eventsListener == null) {
            return;
        }
        this.eventsListener.onChangeFailed(session.id(), workspaceId, c.getNode().getPath());
    }
    @Override
    public void onChangesCount(Watch w, int count) {
        if (this.eventsListener == null) {
            return;
        }
        this.eventsListener.onNewChanges(session.id(), workspaceId, count);
    }

    public void sync(String workspaceId) {
        if (mergeTask != null) {
            mergeTask.stop();
        }

        this.workspaceId = workspaceId;
        try {
            mergeTask = new SyncMergeTask(session, workspaceId);
            mergeTask.start(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //      static access methods
    public static void startSession(Session session) {
        if(session == null) {
            return;
        }

        final boolean isCellsVersion = session.server.versionName().contains("cells");

        synchronized (lock){
            if(instance != null) {
                instance.stop();
            }

            if (!isCellsVersion){
                instance = new OfflineService(session);
            }
        }
    }

    public static void pollChanges(List<WorkspaceNode> workspaceNodes) {
        if (instance != null) {
            instance.start(workspaceNodes);
        }
    }

    public static void stopSession() {
        synchronized (lock) {
            if(instance != null){
                instance.stop();
            }
        }
    }

    public static void setEventsListener(EventsListener listener) {
        synchronized (lock) {
            if(instance == null){
                return;
            }
            instance.eventsListener = listener;
        }
    }

    public static void startWorkspace(String workspaceId) {
        synchronized (lock){
            if(instance == null || instance.session == null){
                return;
            }
            instance.sync(workspaceId);
        }
    }

    public static void watchPath(String path) {
        synchronized (lock) {
            if(instance == null || instance.session == null || instance.workspaceId == null){
                return;
            }
            instance.watch(path);
        }
    }

    public static void unWatchPath(String path) {
        synchronized (lock){
            if(instance == null || instance.session == null || instance.workspaceId == null){
                return;
            }
            instance.unWatch(path);
        }
    }

    public static int watchState(String path) {
        synchronized (lock) {
            if(instance == null || instance.session == null || instance.workspaceId == null){
                return INVALID_SYNC_STATE;
            }

            if(instance.isWatched(path)) {
                return WATCHED;
            }

            if(instance.isUnderAWatched(path)) {
                return UNDER_A_WATCHED;
            }

            return UNWATCHED;
        }
    }
}

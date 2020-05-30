package com.pydio.android.client.services;

import android.content.Context;
import android.os.FileObserver;
import android.widget.Toast;

import java.io.File;

public class FSObserver extends FileObserver{

    private String observedFolder;
    Context context;

    static final int mask =
        FileObserver.CREATE |
        FileObserver.DELETE |
        FileObserver.DELETE_SELF |
        FileObserver.CREATE |
        FileObserver.MODIFY |
        FileObserver.MOVED_FROM |
        FileObserver.MOVED_TO |
        FileObserver.MOVE_SELF;

    public FSObserver(Context c, String path) {
        super(path, mask);
        this.context = c;
    }
    public void onEvent(int event, String path) {
        File file = new File(path);

        String string_event = "";
        switch(event){
            case FileObserver.CREATE:
            case FileObserver.MOVED_TO:
                string_event = "CREATE";
                break;
            case FileObserver.DELETE:
            case FileObserver.MOVED_FROM:
                string_event = "DELETE";
                break;
            case FileObserver.DELETE_SELF:
                //**************
                break;
            case FileObserver.MODIFY:
                string_event = "EDITION";
                break;
            case FileObserver.MOVE_SELF:
                break;
            default:
                break;
        }
        Toast.makeText(context, string_event + ": "+path, Toast.LENGTH_SHORT ).show();
    }
}

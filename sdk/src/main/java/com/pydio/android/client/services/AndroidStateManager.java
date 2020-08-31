package com.pydio.android.client.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.pydio.sdk.core.model.TreeNodeInfo;
import com.pydio.sdk.sync.tree.StateManager;

import java.util.ArrayList;
import java.util.List;

public class AndroidStateManager extends SQLiteOpenHelper implements StateManager {

    public static final int version = 1;

    private static AndroidStateManager instance;

    public static AndroidStateManager get(Context context, String filepath) {
        if (instance == null) {
            instance = new AndroidStateManager(context, filepath);
        }
        return instance;
    }

    private AndroidStateManager(Context context, String filepath) {
        super(context, filepath, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (version == 1) {
            db.execSQL("PRAGMA foreign_keys=off;");
            db.execSQL("create table if not exists dir_path  (" +
                    "id integer not null primary key autoincrement," +
                    "path text not null" +
                    ");");
            db.execSQL("create table if not exists dir_children (" +
                    "parent integer not null," +
                    "name varchar(255) not null," +
                    "encoded text," +
                    "unique (parent, name)," +
                    "foreign key(parent) references dir_path(id) on delete cascade" +
                    ");");
            db.execSQL("PRAGMA foreign_keys=on");

            // Create entry for '/' dir
            ContentValues values = new ContentValues();
            values.put("id", 1);
            values.put("path", "/");
            db.insertWithOnConflict("dir_path", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private long dirID(String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("select id from dir_path where path=?", new String[]{path});
        long id = 0;
        if (c.moveToNext()) {
            id = c.getLong(0);
        }
        c.close();
        return id;
    }

    @Override
    public void put(String path, TreeNodeInfo node) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] pathComponents;

        String parent = "";
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        pathComponents = path.substring(1).split("/");

        for (int i = 0; i < pathComponents.length; i++) {
            String component = pathComponents[i];
            if (i == pathComponents.length - 1) {
                long parentID = 1;
                if (!"".equals(parent) && !"/".equals(parent)) {
                    parentID = this.dirID(parent);
                }

                String encoded = new Gson().toJson(node);

                ContentValues values = new ContentValues();
                values.put("parent", parentID);
                values.put("name", node.getName());
                values.put("encoded", encoded);

                db.insertWithOnConflict("dir_children", "", values, SQLiteDatabase.CONFLICT_REPLACE);

                if (!node.isLeaf()) {
                    db.insertWithOnConflict("dir_path", "", values, SQLiteDatabase.CONFLICT_IGNORE);
                }

            } else {
                String dir = parent + "/" + component;
                if (this.dirID(dir) == 0) {
                    ContentValues values = new ContentValues();
                    values.put("dir", dir);
                    db.insertWithOnConflict("dir_path", "", values, SQLiteDatabase.CONFLICT_IGNORE);
                }
                parent = dir;
            }
        }
    }

    @Override
    public TreeNodeInfo get(String path) {
        String parent = "/";
        String name = "";

        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] pathComponents = path.split("/");

        for (int i = 0; i < pathComponents.length; i++) {
            if (i < pathComponents.length -1) {
                parent = parent.concat("/").concat(pathComponents[i]) ;
            } else {
                name = pathComponents[i];
            }
        }

        long parentId = this.dirID(parent);
        if (parentId == 0) {
            return null;
        }

        SQLiteDatabase rdb = this.getReadableDatabase();
        Cursor c = rdb.rawQuery("select encoded from dir_children where parent=? and name=?", new String[]{""+parentId, name});
        if (!c.moveToNext()) {
            c.close();
            return null;
        }

        String encoded = c.getString(0);
        TreeNodeInfo info = TreeNodeInfo.fromEncoded(encoded);

        c.close();
        return info;
    }

    @Override
    public void remove(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] pathComponents = path.substring(1).split("/");
        String name = pathComponents[pathComponents.length - 1];

        String parent = "";
        long parentID = 1;

        for (int i = 0; i < pathComponents.length - 1; i++) {
            if (i < pathComponents.length - 1) {
                parent = parent.concat("/").concat(pathComponents[i]);
            }
        }

        if ("".equals(parent)) {
            parent = "/";
        } else {
            parentID = this.dirID(parent);
            if (parentID == 0) {
                return;
            }
        }

        SQLiteDatabase db = getWritableDatabase();
        db.delete("path_children", "parent=? and name=?", new String[]{""+ parent, name});

        // delete the matching path in dirs
        db.delete("dir_path", "path=?", new String[]{path});
    }

    @Override
    public List<TreeNodeInfo> getChildren(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] pathComponents = path.substring(1).split("/");
        String name = pathComponents[pathComponents.length - 1];

        String parent = "";
        long parentID = 1;

        for (int i = 0; i < pathComponents.length - 1; i++) {
            if (i < pathComponents.length - 1) {
                parent = parent.concat("/").concat(pathComponents[i]);
            }
        }

        if (!"".equals(parent) && !"/".equals(parent)) {
            parentID = this.dirID(parent);
            if (parentID == 0) {
                return new ArrayList<TreeNodeInfo>();
            }
        }

        Cursor c = this.getReadableDatabase().rawQuery("select encoded from dir_children where parent=? and name=? order by name", new String[]{""+parentID, name});
        List<TreeNodeInfo> infoList = new ArrayList<>();
        while(c.moveToNext()) {
            String encoded = c.getString(0);
            infoList.add(TreeNodeInfo.fromEncoded(encoded));
        }
        c.close();
        return infoList;
    }
}

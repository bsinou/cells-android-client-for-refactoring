package com.pydio.android.client.services;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;

import com.google.gson.Gson;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.db.Schema;
import com.pydio.android.client.data.images.Thumb;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.common.callback.NodeHandler;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Node;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

public class Cache {

    private final static Object lock = new Object();

    private static SQLiteDatabase writeDB() {
        return DBOpenHelper().getWritableDatabase();
    }

    private static SQLiteDatabase readDB(){
        return DBOpenHelper().getReadableDatabase();
    }

    private static void closeDB(SQLiteDatabase db) {
        if (db != null) {
            db.close();
            db.releaseReference();
        }
    }

    public static int countNodes(String workspace, final String path) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            String[] args = new String[]{workspace, path};
            Cursor cursor = db.rawQuery(Schema.nodesCountSQL, args);
            int count = cursor.getInt(0);
            cursor.close();
            return count;
        }
    }

    public static int nodes(String workspace, final String path, final NodeHandler handler) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            String sql = Schema.nodeListSQL;
            Cursor cursor = db.rawQuery(sql, new String[]{workspace, path});
            if (!cursor.moveToFirst()) {
                cursor.close();
                return -1;
            }

            int count = cursor.getCount();
            do {
                byte[] blob = cursor.getBlob(1);
                String json = new String(blob, Charset.defaultCharset());
                Node node = new Gson().fromJson(json, FileNode.class);
                node.setProperty(Pydio.NODE_PROPERTY_ID, String.valueOf(cursor.getInt(0)));
                String slug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
                if (slug == null || "".equals(slug)) {
                    node.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspace.split("\\.")[1]);
                }
                handler.onNode(node);
            } while (cursor.moveToNext());
            cursor.close();
            return count;
        }
    }

    public static Node get(String workspace, String path, String name) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Cursor cursor = db.rawQuery(Schema.nodeGetSQL, new String[]{Schema.col_blob, workspace, path, name});
            if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

            byte[] blob = cursor.getBlob(1);
            cursor.close();

            String json = new String(blob, Charset.defaultCharset());
            Node node = new Gson().fromJson(json, Node.class);
            node.setProperty(Pydio.NODE_PROPERTY_ID, String.valueOf(cursor.getInt(0)));
            return node;
        }
    }

    public static long addNode(String workspace, String path, Node node) {
        synchronized (lock) {
            SQLiteDatabase wdb = writeDB();
            Gson gson = new Gson();
            String encoded = gson.toJson(node);
            byte[] bytes = encoded.getBytes(Charset.defaultCharset());

            ContentValues cv = new ContentValues();
            cv.put(Schema.col_ws, workspace);
            cv.put(Schema.col_path, path);
            cv.put(Schema.col_filename, node.label());
            cv.put(Schema.col_mtime, node.getProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME));
            cv.put(Schema.col_blob, bytes);

            long id = wdb.insert(Schema.nodes, null, cv);
            closeDB(wdb);
            return id;
        }
    }

    public static void addNodes(String workspace, String parent, List<Node> nodes) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            for (Node node : nodes) {
                String path = node.path();
                String name = node.label();
                if (parent == null || "".equals(parent)) {
                    parent = "/";
                }
                Gson gson = new Gson();
                String encoded = gson.toJson(node);
                byte[] bytes = encoded.getBytes(Charset.defaultCharset());

                Object[] params = new Object[]{workspace, parent, name, node.getProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME), bytes};
                db.execSQL(Schema.addNodeSQL, params);
            }
            closeDB(db);
        }
    }

    public static void update(String workspace, String path, String oldNodeLabel, Node newNode) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Gson gson = new Gson();
            String encoded = gson.toJson(newNode);
            byte[] bytes = encoded.getBytes(Charset.defaultCharset());

            Object[] params = new Object[]{bytes, newNode.label(), newNode.getProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME), newNode.label(), workspace, path, oldNodeLabel};
            db.execSQL(Schema.updateNodeSQL, params);
            closeDB(db);
        }
    }

    public static void updateNodes(String workspace, String path, List<Node> nodes) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            for (Node node: nodes) {
                Gson gson = new Gson();
                String encoded = gson.toJson(node);
                byte[] bytes = encoded.getBytes(Charset.defaultCharset());
                Object[] params = new Object[]{bytes, node.label(), node.getProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME), node.label(), workspace, path, node.label()};
                db.execSQL(Schema.updateNodeSQL, params);
            }
            closeDB(db);
        }
    }

    public static void deleteNode(String workspace, String path, String name) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Object[] params = new Object[]{workspace, path, name};
            db.execSQL(Schema.deleteNodeSQL, params);
            closeDB(db);
        }
    }

    public static void deleteNodes(String workspace, List<Node> nodes) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            for (Node node: nodes) {
                String path = node.path();
                String name = new File(path).getName();
                String parent = new File(path).getParent();
                if (parent == null || "".equals(parent)) {
                    parent = "/";
                }
                Object[] params = new Object[]{workspace, parent, node.label()};
                db.execSQL(Schema.deleteNodeSQL, params);
            }
            closeDB(db);
        }
    }

    public static void clearFolder(String workspace, String folder) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Object[] params = new Object[]{workspace, folder};
            db.execSQL(Schema.clearDirectoryNodesSQL, params);
            closeDB(db);
        }
    }

    public static void clear() {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            db.execSQL(Schema.clearNodesSQL);
            closeDB(db);
        }
    }

    public static void clear(String workspace) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            db.execSQL(Schema.clearWorkspaceNodesSQL, new Object[]{workspace});
            closeDB(db);
        }
    }

    public static void clear(String workspace, String path) {
        synchronized (lock) {
            SQLiteDatabase wdb = writeDB();
            wdb.execSQL(Schema.clearDirectoryNodesSQL, new Object[]{workspace, path + "%"});
            closeDB(wdb);
        }
    }

    public static void saveThumb(String workspace, String nodePath, String thumbnailPath, long editTime, int dim) {
        synchronized (lock) {
            if (workspace == null) return;
            SQLiteDatabase db = writeDB();
            db.execSQL(Schema.deleteThumbSQL, new Object[]{workspace, nodePath, String.valueOf(dim)});
            db.execSQL(Schema.insertThumbSQL, new Object[]{workspace, nodePath, thumbnailPath, String.valueOf(editTime), String.valueOf(dim)});
            closeDB(db);
        }
    }

    public static Thumb getThumbnail(String localWsId, String path, int dim) {
        synchronized (lock) {
            Cursor cursor = writeDB().rawQuery(Schema.getThumbPathSQL, new String[]{localWsId, path, String.valueOf(dim)});
            if (cursor.getCount() == 0 || !cursor.moveToFirst()) {
                return null;
            }

            String link = cursor.getString(0);
            long mTime = Long.parseLong(cursor.getString(1));

            cursor.close();
            Thumb t = new Thumb();
            t.editTime = mTime;
            t.path = link;
            t.bitmap = BitmapFactory.decodeFile(link);
            return t;
        }
    }

    public static String[] searchWords(String localWsID) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Cursor cursor = db.rawQuery(Schema.searchNodesSQL, new String[]{localWsID, "search://%"});
            if (!cursor.moveToFirst()) {
                return new String[0];
            }
            int count = cursor.getCount();
            String[] result = new String[count];
            int index = 0;
            do {
                result[index] = cursor.getString(0).substring("search://".length());
                index++;
            } while (cursor.moveToNext());
            cursor.close();
            return result;
        }
    }

    private static Schema DBOpenHelper() {
        return Schema.getInstance(Application.context());
    }
}

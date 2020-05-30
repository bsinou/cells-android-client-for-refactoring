package com.pydio.android.client.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pydio.android.client.data.Application;

public class Schema extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;

    public static Schema instance;

    public static Schema getInstance(Context context) {
        if (instance == null) {
            instance = new Schema(context, Application.CACHE_DATABASE_FILE);
        }
        return instance;
    }

    private Schema(Context context, String path) {
        super(context, path, null, DB_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createNodes);
        db.execSQL(createThumbs);
        db.execSQL(createProps);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        try{db.execSQL("DROP TABLE `node_temp_cache`");}catch (Exception ignored){}
        try{db.execSQL("DROP TABLE `node_cache`");}catch (Exception ignored){}
        try{db.execSQL("DROP TABLE `file_cache`");}catch (Exception ignored){}


        db.execSQL(createThumbs);
        db.execSQL(createProps);
        db.execSQL(createNodes);
    }

    public static final String props = "props_cache";
    public static final String nodes = "node_cache";
    public static final String files = "file_cache";
    public static final String thumbs = "thumbnails";

    public static final String col_node_id = "node_id";
    public static final String col_filename = "filename";
    public static final String col_file_path = "file_path";
    public static final String col_mtime = "last_modifed";
    public static final String col_ws = "workspace_id";
    public static final String col_path = "path";
    public static final String col_dim = "thumb_dim";
    public static final String col_prop_name = "props_name";
    public static final String col_blob = "content";

    private final String createProps = String.format("create table if not exists %s (%s varchar(255) not null, %s blob not null);", props, col_prop_name, col_blob);
    private final String createNodes = String.format("create table if not exists %s (%s integer primary key autoincrement, %s text not null, %s text not null, %s text not null, %s integer, %s blob not null);", nodes, col_node_id, col_ws, col_path, col_filename, col_mtime, col_blob);
    private final String createFiles = String.format("create table if not exists %s (%s text not null, %s text not null, %s text not null, %s text not null, %s integer, unique (%s, %s, %s));", files, col_ws, col_path, col_filename, col_file_path, col_mtime, col_ws, col_path, col_filename);
    private final String createThumbs = String.format("create table if not exists %s (%s text not null, %s text not null, %s text not null, %s text not null, %s text not null, unique (%s, %s, %s));", thumbs, col_ws, col_path, col_file_path, col_mtime, col_dim, col_ws, col_path, col_dim);

    final static public String nodeListSQL = String.format("select %s, %s from %s where %s=? and %s=?;", col_node_id, col_blob, nodes, col_ws, col_path);
    final static public String nodeIdSQL = String.format("select %s from %s where %s=? and %s=? and %s=?;", col_node_id, nodes, col_ws, col_path, col_filename);
    final static public String nodesCountSQL = String.format("select count(*) from %s=? where %s=? and %s=?", nodes, col_ws, col_path);
    final static public String nodeGetSQL = String.format("select %s, %s from %s=? where %s=? and %s=? and %s=?", col_node_id, col_blob, nodes, col_ws, col_path, col_filename);
    final static public String addNodeSQL = String.format("insert into %s (%s, %s, %s, %s, %s) values(?, ?, ?, ?, ?);", nodes, col_ws, col_path, col_filename, col_mtime, col_blob);
    final static public String updateNodeSQL = String.format("update %s set %s=?, %s=?, %s=?, %s=? where %s=? and %s=? and %s=?;", nodes, col_blob, col_filename, col_mtime, col_filename, col_ws, col_path, col_filename);
    final static public String deleteNodeSQL = String.format("delete from %s where %s=? and %s=? and %s=?;", nodes, col_ws, col_path, col_filename);
    final static public String clearNodesSQL = String.format("delete from %s;", nodes);
    final static public String clearWorkspaceNodesSQL = String.format("delete from %s where %s=?;", nodes, col_ws);
    final static public String clearDirectoryNodesSQL = String.format("delete from %s where %s=? and %s like ?;", nodes, col_ws, col_path);

    final static public String clearDirectoryFilesSQL = String.format("delete from %s where %s=? and %s like ?;", files, col_ws, col_path);
    final static public String listWorkspaceFilesSQL = String.format("select %s from %s where %s=? and %s like ?;", col_path, files, col_ws, col_path);

    final static public String deleteThumbSQL = String.format("delete from %s where %s=? and %s=? and %s=?;", thumbs, col_ws, col_path, col_dim);
    final static public String insertThumbSQL = String.format("insert into %s values (?, ?, ?, ?, ?);", thumbs);
    final static public String getThumbPathSQL = String.format("select %s, %s from %s where %s=? and %s=? and %s=?;", col_file_path, col_mtime, thumbs, col_ws, col_path, col_dim);

    final static public String searchNodesSQL = String.format("select %s from %s where %s=? and %s like ?;", col_path, nodes, col_ws, col_path);
}

package com.pydio.android.client.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.google.gson.Gson;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.transfers.Transfer;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.auth.Token;
import com.pydio.sdk.core.security.LegacyPasswordManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {

    private static Database instance;

    public final static int VERSION = 1;

    private static final Object lock = new Object();

    public static Database getHelper(Context context) {
        if (instance == null) {
            instance = new Database(context.getApplicationContext(), Application.DATABASE_FILE, VERSION);
        }
        return instance;
    }

    private static SQLiteDatabase writeDB() {
        return getHelper(Application.context()).getWritableDatabase();
    }

    private static SQLiteDatabase readDB() {
        return getHelper(Application.context()).getReadableDatabase();
    }

    public static Database instance() {
        return getHelper(Application.context());
    }



    //***************************************************************************************
    //              SQLite Helper
    //***************************************************************************************
    public Database(Context context, String path, int version) {
        super(context, path, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(create_cookies);
        db.execSQL(create_offlines);
        db.execSQL(create_properties);
        db.execSQL(create_seq);
        db.execSQL(create_changes);
        db.execSQL(create_certificate);
        db.execSQL(createSessions);
        db.execSQL(createTokens);
        db.execSQL(createTransfers);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*try {
            db.execSQL("drop table `sessions`;");
        } catch (Exception ignore) {}*/
        db.execSQL(createSessions);
        db.execSQL(createTokens);
        db.execSQL(createTransfers);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    public static void saveCertificate(String alias, X509Certificate cert) {
        instance().addCertificate(alias, cert);
    }

    public void addCertificate(String alias, X509Certificate cert) {
        synchronized (lock) {
            try {
                SQLiteDatabase db = Database.getHelper(Application.context()).getWritableDatabase();
                String sql = "INSERT INTO " + certificates + " values(?, ?)";
                SQLiteStatement s = db.compileStatement(sql);
                s.bindString(1, alias);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(cert);
                byte[] data = bos.toByteArray();
                bos.close();
                s.bindBlob(2, data);
                s.execute();
                db.close();
            } catch (Exception e) {
            }
        }
    }

    public static List<X509Certificate> getTrustedCertificates() {
        synchronized (lock) {
            Cursor cursor;
            List<X509Certificate> certificateList = new ArrayList<>();
            try {
                cursor = Database.getHelper(Application.context()).getReadableDatabase().query(certificates, new String[]{col_certificate}, null, null, null, null, null);
                for (; cursor.moveToNext(); ) {
                    byte[] certificateBytes = cursor.getBlob(0);
                    ByteArrayInputStream bis = new ByteArrayInputStream(certificateBytes);
                    ObjectInput in = new ObjectInputStream(bis);
                    X509Certificate cert = (X509Certificate) in.readObject();
                    bis.close();
                    certificateList.add(cert);
                }
                cursor.close();
            } catch (Exception ignored) {
            }
            return certificateList;
        }
    }

    //***************************************************************************************
    //              CERTIFICATES
    //***************************************************************************************
    public X509Certificate getCertificate(String alias) {
        synchronized (lock){
            Cursor cursor;
            try {
                cursor = Database.getHelper(Application.context()).getReadableDatabase().query(certificates, new String[]{col_certificate}, col_alias + "=?", new String[]{alias}, null, null, null);
                if (!cursor.moveToFirst()) {
                    return null;
                }
                byte[] certificateBytes = cursor.getBlob(0);
                ByteArrayInputStream bis = new ByteArrayInputStream(certificateBytes);
                ObjectInput in = new ObjectInputStream(bis);
                X509Certificate cert = (X509Certificate) in.readObject();
                bis.close();
                cursor.close();
                return cert;
            } catch (Exception ignored) {
            }
            return null;
        }
    }


    //***************************************************************************************
    //              PASSWORDS
    //***************************************************************************************
    public static String[] users() {
        synchronized (lock){
            Cursor cursor = Database.getHelper(Application.context()).getReadableDatabase().query(cookies, new String[]{col_user}, null, null, null, null, null);
            if (!cursor.moveToFirst()) {
                return null;
            }
            String[] result = new String[cursor.getCount()];
            int i = 0;
            boolean can_browse = true;
            while (can_browse) {
                result[i] = cursor.getString(0);
                i++;
                can_browse = cursor.moveToNext();
            }
            return result;
        }
    }

    public static String password(String user) {
        synchronized (lock){
            try {
                Cursor cursor = Database.getHelper(Application.context()).getReadableDatabase().query(cookies, new String[]{col_password}, col_user + "=?", new String[]{user}, null, null, null);
                if (!cursor.moveToFirst()) {
                    return null;
                }
                String password = cursor.getString(0);
                cursor.close();

                if (!password.startsWith("$AJXP_ENC$")) {
                    updatePassword(user, user, password);
                }
                return LegacyPasswordManager.decrypt(password);
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    public static void addPassword(String user, String password) {
        synchronized (lock){
            String encPassword;
            try {
                encPassword = LegacyPasswordManager.encrypt(password);
            } catch (GeneralSecurityException e) {
                encPassword = password;
            }
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(col_user, user);
            values.put(col_password, encPassword);
            SQLiteDatabase db = Database.getHelper(Application.context()).getWritableDatabase();
            db.insertWithOnConflict(cookies, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
        }
    }

    public static void updatePassword(String user, String newUser, String newPassword) {
        synchronized (lock){
            String encNewPassword;
            try {
                encNewPassword = LegacyPasswordManager.encrypt(newPassword);
            } catch (GeneralSecurityException e) {
                encNewPassword = newPassword;
            }

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(col_user, newUser);
            values.put(col_password, encNewPassword);
            SQLiteDatabase db = Database.getHelper(Application.context()).getWritableDatabase();
            db.update(cookies, values, col_user + "=?", new String[]{user});
            db.close();
        }
    }

    public static void deletePassword(String user) {
        synchronized (lock){
            SQLiteDatabase db = Database.getHelper(Application.context()).getWritableDatabase();
            db.delete(cookies, String.format("%s='%s'", col_user, user), null);
            db.close();
        }
    }

    public static void deletePasswords(String server) {
        synchronized (lock){
            SQLiteDatabase db = Database.getHelper(Application.context()).getWritableDatabase();
            db.delete(cookies, String.format("%s='%s'", col_user, "%" + server.replace("://", "+").replace("/", "&")), null);
            db.close();
        }
    }


    //***************************************************************************************
    //              PROPERTIES
    //***************************************************************************************
    public static String getProperty(String key) {
        synchronized (lock){
            return getProperty(key, null);
        }
    }

    public static void setProperty(String name, String value) {
        synchronized (lock){
            try {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(col_name, name);
                values.put(col_value, value);
                SQLiteDatabase db = Database.getHelper(Application.context()).getWritableDatabase();
                db.insertWithOnConflict(properties, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                db.close();
            } catch (Exception e) {
                //Log.e("Cache", e.getMessage());
            }
        }
    }

    public static String getProperty(String key, String defaultValue) {
        synchronized (lock){
            try {
                Cursor cursor = Database.getHelper(Application.context()).getReadableDatabase().query(properties, new String[]{col_value}, col_name + "=?", new String[]{key}, null, null, null);
                if (!cursor.moveToFirst()) {
                    return null;
                }
                String password = cursor.getString(0);
                cursor.close();
                return password;
            } catch (Exception e) {
                //Log.e("Cache", e.getMessage());
                return defaultValue;
            }
        }
    }


    //***************************************************************************************
    //              SESSIONS
    //***************************************************************************************
    public static boolean saveSession(Session s) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();

            Gson gson = new Gson();
            String encoded = gson.toJson(s);
            byte[] bytes = encoded.getBytes(Charset.defaultCharset());
            db.execSQL(deleteSessionSQL, new String[]{s.id()});
            db.execSQL(saveSessionSQL, new Object[]{s.id(), bytes});
            db.close();
            return true;
        }
    }

    public static void deleteSession(String id) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            db.execSQL(deleteSessionSQL, new String[]{id});
            db.close();
        }
    }

    public static Session getSession(String id) {
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            Cursor c = db.rawQuery(getSessionSQL, new String[]{id});
            if (!c.moveToNext()) {
                c.close();
                return null;
            }

            byte[] blob = c.getBlob(0);
            c.close();
            String json = new String(blob, Charset.defaultCharset());
            return new Gson().fromJson(json, Session.class);
        }
    }

    public static List<Session> listSessions() {
        synchronized (lock) {
            List<Session> sessions = new ArrayList<>();
            SQLiteDatabase db = readDB();
            Cursor cursor = db.rawQuery(listSessionsSQL, null);
            while (cursor.moveToNext()) {
                byte[] blob = cursor.getBlob(0);
                String json = new String(blob, Charset.defaultCharset());
                if (json.contains("mHost")) {
                    json = json.replace("mHost", "host")
                            .replace("mScheme", "scheme")
                            .replace("mPort", "port")
                            .replace("mPath", "path")
                            .replace("mVersion", "version")
                            .replace("mVersionName", "versionName")
                            .replace("mIconURL", "iconURL")
                            .replace("mWelcomeMessage", "welcomeMessage")
                            .replace("mLabel", "label")
                            .replace("mUrl", "url")
                            .replace("mSSLContext", "sslContext")
                            .replace("mSSLUnverified", "sslUnverified")
                            .replace("mLegacy", "legacy")
                            .replace("mProperties", "properties");
                }
                Session s = new Gson().fromJson(json, Session.class);
                sessions.add(s);
            }
            cursor.close();
            return sessions;
        }
    }

    public static void saveToken(Token t) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            db.execSQL(delJWTSQL, new String[]{t.subject});
            db.execSQL(saveJWTSQL, new Object[]{t.subject, Token.encode(t)});
            db.close();
        }
    }

    public static Token getToken(String subject) {
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            Cursor c = db.rawQuery(getJWTSQL, new String[]{subject});
            if (!c.moveToNext()) {
                c.close();
                return null;
            }

            String serialized = c.getString(0);
            c.close();
            return Token.decode(serialized);
        }
    }

    public static void deleteToken(String subject) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            db.execSQL(delJWTSQL, new String[]{subject});
            db.close();
        }
    }

    //***************************************************************************************
    //              TRANSFERS
    //***************************************************************************************
    public static void addTransfer(Transfer t) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Object[] params = new Object[]{t.getSession(), t.getWorkspace(), t.getType(), t.getRemote(), t.getLocal(), t.getSize(), t.getStatus()};
            db.execSQL(addTransferSQL, params);
            db.close();
        }
    }

    public static void addTransfers(List<Transfer> list) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            for (Transfer t : list) {
                Object[] params = new Object[]{t.getSession(), t.getWorkspace(), t.getType(), t.getRemote(), t.getLocal(), t.getSize(), t.getStatus()};
                db.execSQL(addTransferSQL, params);
            }
            db.close();
        }
    }

    public static void deleteTransfer(long id) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Object[] params = new Object[]{id};
            db.execSQL(deleteTransferSQL, params);
            db.close();
        }
    }

    public static void deleteTransfersByStatus(int status) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Object[] params = new Object[]{status};
            db.execSQL(deleteTransfersByStatusSQL, params);
            db.close();
        }
    }

    public static void clearTransfers() {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            db.execSQL(clearTransfersSQL, null);
            db.close();
        }
    }

    public static void updateTransferStatus(long id, int status) {
        synchronized (lock) {
            SQLiteDatabase db = writeDB();
            Object[] params = new Object[]{status, id};
            db.execSQL(updateTransferStatusSQL, params);
            db.close();
        }
    }

    public static Transfer dequeueTransfer() {
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            Cursor c = db.rawQuery(selectTransferSQL, null);
            Transfer t = null;
            if (c.moveToNext()) {
                t = Transfer.parse(c);
            }
            c.close();
            return t;
        }
    }


    //***************************************************************************************
    //              SCHEMA
    //***************************************************************************************
    //offline tables
    public static final String index = "`index_group`";

    public static final String offline_tasks = "`offline_tasks`";
    public static final String offline_events = "`offline_events`";
    public static final String offline_folders = "`offline_folders`";
    public static final String session_folders = "`session_folders`";
    public static final String seqs = "`changes_seq`";
    public static final String changes = "`changes`";
    public static final String certificates = "`certificates`";
    public static final String cookies = "`cookies`";
    public static final String properties = "`properties`";
    public static final String tokens = "`tokens`";
    public static final String transfers = "`transfers`";
    public static final String sessions = "`sessions`";

    public static final String col_transfer_id = "`transfer_id`";
    public static final String col_type = "`type`";
    public static final String col_status = "`status`";
    public static final String col_local = "`local`";
    public static final String col_remote = "`remote`";
    public static final String col_size = "`user`";


    public static final String col_user = "`user`";
    public static final String col_password = "`password`";
    public static final String col_jwt = "`jwt`";
    public static final String col_workspace = "`workspace_id`";
    public static final String col_path = "`path`";
    public static final String col_name = "`name`";
    public static final String col_value = "`value`";
    public static final String col_alias = "`alias`";
    public static final String col_certificate = "`certificate`";
    public static final String col_session_id = "`session_id`";
    public static final String col_task_id = "`task_id`";
    public static final String col_folder_name = "`folder_name`";
    public static final String col_address = "`address`";
    public static final String col_node = "`node`";
    public static final String col_display_name = "`user_display_name`";
    public static final String col_logo = "`logo`";
    public static final String col_session_name = "`session_name`";


    public static final String col_change_seq = "`" + Pydio.CHANGE_SEQ + "`";
    public static final String col_change_type = "`" + Pydio.CHANGE_TYPE + "`";
    public static final String col_change_source = "`" + Pydio.CHANGE_SOURCE + "`";
    public static final String col_change_target = "`" + Pydio.CHANGE_TARGET + "`";
    public static final String col_node_bytesize = "`" + Pydio.CHANGE_NODE_BYTESIZE + "`";
    public static final String col_change_md5 = "`" + Pydio.CHANGE_NODE_MD5 + "`";
    public static final String col_change_mtime = "`" + Pydio.CHANGE_NODE_MTIME + "`";
    public static final String col_change_node_path = "`" + Pydio.CHANGE_NODE_PATH + "`";
    public static final String col_change_location = "`location`";
    public static final String col_change_node_id = "`" + Pydio.CHANGE_NODE_ID + "`";
    public static final String col_task_state = "`state`";
    public static final String col_error_source = "`error_source`";
    public static final String col_error = "`error`";
    public static final String col_blob = "`content`";
    public static final String col_sid = "`session_id`";


    private static final String
            create_cookies =
            "CREATE TABLE " + cookies + " (" +
                    col_user + " TEXT PRIMARY KEY NOT NULL, " +
                    col_password + " TEXT" +
                    "); ",

    create_offlines =
            "CREATE TABLE " + index + "(" +
                    col_workspace + " TEXT NOT NULL, " +
                    col_path + " TEXT NOT NULL, " +
                    col_change_seq + " INTEGER DEFAULT 0, " +
                    "UNIQUE (" + col_workspace + ", " + col_path + ")" +
                    ");",

    create_changes = "" +
            "CREATE TABLE " + changes + "( " +
            col_change_seq + " INTEGER, " +
            col_change_node_id + " INTEGER, " +
            col_change_type + " TEXT, " +
            col_change_source + " TEXT, " +
            col_change_target + " TEXT, " +
            col_node_bytesize + " INTEGER," +
            col_change_md5 + " TEXT, " +
            col_change_mtime + " INTEGER, " +
            col_change_node_path + "TEXT, " +
            col_workspace + " TEXT NOT NULL, " +
            col_change_location + " TEXT NOT NULL" +
            ");",

    create_properties =
            "CREATE TABLE " + properties + "(" +
                    col_name + " TEXT PRIMARY KEY NOT NULL," +
                    col_value + " TEXT);",

    create_certificate =
            "CREATE TABLE " + certificates + "(" +
                    col_alias + " TEXT PRIMARY KEY NOT NULL, " +
                    col_certificate + " BLOB NOT NULL); ",

    create_seq =
            "CREATE TABLE " + seqs + "(" +
                    col_workspace + " TEXT PRIMARY KEY NOT NULL, " +
                    col_change_seq + " INTEGER" +
                    ");";

    private final String createSessions = String.format("create table if not exists %s (%s text not null, %s blob not null);", sessions, col_sid, col_blob);
    private final static String deleteSessionSQL = String.format("delete from %s where %s=?;", sessions, col_sid);
    private final static String getSessionSQL = String.format("select %s from %s where %s=?;", col_blob, sessions, col_sid);
    private final static String saveSessionSQL = String.format("insert into %s values(?, ?);", sessions);
    final static public String listSessionsSQL = String.format("select %s from %s;", col_blob, sessions);

    private final String createTokens = String.format("create table if not exists %s (%s text not null, %s text not null);", tokens, col_sid, col_jwt);
    final static public String saveJWTSQL = String.format("insert into %s values (?, ?);", tokens);
    final static public String getJWTSQL = String.format("select %s from %s where %s=?;", col_jwt, tokens, col_sid);
    final static public String delJWTSQL = String.format("delete from %s where %s=?;", tokens, col_sid);

    private final String createTransfers = String.format(
            "create table if not exists %s (" +
                    "%s integer primary key autoincrement, " +
                    "%s text not null, " + //sessionId
                    "%s text not null, " + //workspace
                    "%s integer not null, " + //type
                    "%s text not null, " + //remote
                    "%s text not null, " + //local
                    "%s text not null, " + //size
                    "%s text not null" + //status
                    ");",
            transfers, col_transfer_id, col_sid, col_workspace, col_type, col_remote, col_local, col_size, col_status);
    final static public String addTransferSQL = String.format("insert into %s (%s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?);", transfers, col_sid, col_workspace, col_type, col_remote, col_local, col_size, col_status);
    final static public String selectTransferSQL = String.format("select * from %s order by %s asc limit 1;", transfers, col_transfer_id);
    final static public String deleteTransferSQL = String.format("delete from %s where %s=?;", transfers, col_transfer_id);
    final static public String updateTransferStatusSQL = String.format("update %s set %s=? where %s=?;", transfers, col_status, col_transfer_id);
    final static public String deleteTransfersByStatusSQL = String.format("delete from %s where %s=?;", transfers, col_status);
    final static public String clearTransfersSQL = String.format("delete from %s;", transfers);
}

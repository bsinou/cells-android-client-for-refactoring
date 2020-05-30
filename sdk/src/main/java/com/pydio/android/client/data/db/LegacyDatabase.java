package com.pydio.android.client.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.pydio.android.client.data.Application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class LegacyDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ajaxplorer_cache.db";
    private static int VERSION = 1;
    private static LegacyDatabase mInstance;
    private static Context mContext;
    private static String mPath;


    public static LegacyDatabase get(Context c) throws IOException{
        mContext = c;
        mPath = c.getFilesDir().getParentFile() + File.separator + "databases" + File.separator + DATABASE_NAME;
        //String path = Application.downloadsDir() + File.separator + "ajxp_database.db";
        //String path = Environment.getExternalStorageDirectory() + "/Downloads/" + DATABASE_NAME;
        if(!new File(mPath).exists()) throw new IOException();
        if(mInstance == null){
            mInstance = new LegacyDatabase(c, mPath, VERSION);
        }
        return mInstance;
    }

    private LegacyDatabase(Context context, String path, int version) {
        super(context, path, null, version);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {}
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public ArrayList<Properties> servers(){
        synchronized (mInstance) {
            //tables();
            //getWritableDatabase().close();
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM b WHERE name IN ('url','user','password','trust_ssl','expire','id','statusLabel','Resolution_Alias') ORDER BY node_id DESC", null);
            if (!c.moveToFirst()) {
                return null;
            }

            ArrayList<Properties> servers = new ArrayList<Properties>();
            int index = -1;
            int nodeId = 0;
            Properties p = null;

            do{
                int cNodeId = c.getInt(c.getColumnIndex("node_id"));
                String name = c.getString(c.getColumnIndex("name"));
                String value = c.getString(c.getColumnIndex("value"));
                if (cNodeId != nodeId) {
                    nodeId = cNodeId;
                    index++;
                    servers.add(p = new Properties());
                }
                p.setProperty(name, value);
            } while(c.moveToNext());
            c.close();
            return servers;
        }
    }

    public String[] tables(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        if (c.moveToFirst()) {
            int index = 0;
            String[] tables = new String[c.getCount()];
            do{
                tables[index] = c.getString(0);
                index++;
            }while(c.moveToNext());
            return tables;
        }
        return new String[0];
    }

    public void destroy(){
        try {
            SQLiteDatabase db = getWritableDatabase();
            //db.delete("b");
            db.close();
            String path = mContext.getFilesDir().getParentFile().getPath() + "/databases/" + DATABASE_NAME;
            Application.localSystem.delete(path);
        }catch (Exception e){
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

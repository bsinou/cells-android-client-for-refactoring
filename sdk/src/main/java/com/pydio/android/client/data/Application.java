package com.pydio.android.client.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;

import com.pydio.android.client.R;
import com.pydio.android.client.accounts.Accounts;
import com.pydio.android.client.data.callback.SessionCompletion;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.data.encoding.B64;
import com.pydio.android.client.data.extensions.AndroidCellsClient;
import com.pydio.android.client.data.extensions.AndroidClientFactory;
import com.pydio.android.client.data.metrics.Measurement;
import com.pydio.android.client.gui.activities.UserCredentials;
import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.ApplicationData;
import com.pydio.sdk.core.ClientFactory;
import com.pydio.sdk.core.PydioCells;
import com.pydio.sdk.core.common.callback.Completion;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.model.ServerNode;
import com.pydio.sdk.core.security.Passwords;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class Application extends android.app.Application {

    public static final String ENTERPRISE_ID = "enterprise_id";
    private static String PERSISTENCE_KEY;
    public static final String OFFLINE_FOLDER = "offline";
    public static final String CACHE_FOLDER = "cache";
    public static final String FILES = "files";
    public static final String TEMP_FOLDER = "temp";
    public static final String DOWNLOADS_FOLDER = "downloads";
    public static String DATABASE_FILE;
    public static String DATABASE_THUMBNAIL_CACHE;
    public static String CACHE_DATABASE_FILE;
    public static final String PREF_SORT_CRITERIA = "criteria";

    public static final String PREF_SORT_ORDER = "order";
    public static final String PREF_CACHE_FOLDER = "CACHE_FOLDER";
    public static final String PREF_CACHE_SIZE = "CACHE_FOLDER_SIZE";
    public static final String PREF_CACHE_PRUNE_INTERVAL = "PRUNING_EVERY";
    public static final String PREF_CACHE_ENABLED = "CACHE_ENABLED";
    public static final String PREF_DOWNLOAD_FOLDER = "DOWNLOAD_FOLDER";
    public static final String PREF_TRANSFER_CONNECTION = "PREF_TRANSFER_CONNECTION";
    public static final String PREF_MASTER_PASSWORD_ENABLED = "MASTER_PASSWORD_ENABLED";
    public static final String PREF_MASTER_PERSISTENCE = "MASTER_PASSWORD_PERSISTENCE";
    public static final String PREF_DISPLAY_MODE = "DISPLAY_MODE";
    public static final String PREF_FIRST_TIME = "FIRST_TIME";
    public static final String PREF_SERVER_URL = "URL";
    public static final String PREF_SESSION_USER = "USER";
    public static final String PREF_SAVED_SESSION = "sessionId";
    public static final String PREF_WORKSPACE_ID = "WORKSPACE";
    public static final String PREF_WORKSPACE_SECTION = "WORKSPACE_SECTION";
    public static final String PREF_OFFLINE_ENABLED = "OFFLINE_ENABLED";
    public static final String PREF_PIN = "PIN";
    public static final String PREF_PIN_ENABLED = "PIN_ENABLED";
    public static final String PREF_PIN_REQUESTED = "PIN_REQUESTED";
    public static final String PREF_SAVE_PASSWORD = "SAVE_PASSWORD";
    public static final String PREF_NETWORK_3G_PREVIEW = "3G_PREVIEW";
    public static final String PREF_NETWORK_3G_TRANSFER = "3G_TRANSFER";
    public static final String PREF_BACKUP_CONFIG = "BACKUP_CONFIG";
    public static final String PREF_CLEAR_CACHE = "CLEAR_CACHE";
    public static final String PREF_PREVIOUS_SCREEN_HEIGHT = "PREV_SCREEN_HEIGHT";
    public static final String PREF_PREVIOUS_SCREEN_WIDTH = "PREV_SCREEN_WIDTH";
    public static final String PREF_SPLASH_IMAGE_PATH = "SPLASH_IMAGE_PATH";
    public static final String PREF_APP_TITLE = "APP_TITLE";
    public static final String PREF_APP_STATE = "STATE";
    public static final String PREF_BACKGROUND_COLOR = "SPLASH_BG_COLOR";
    public static final String PREF_MAIN_COLOR = "MAIN_COLOR";
    public static final String LICENSE_KEY = "license_key";
    public static final String APPLICATION_LICENSED = "licensed";
    private static final String PREF_MIGRATED = "migration_done";
    public static final int MIN_CACHE_PRUNE_INTERVAL = 1;


    public static String applicationImage;
    public static String applicationName;


    public static PreviewerData previewerActivityData;

    static Timer activityTransitionTimer;
    static TimerTask activityTransitionTimerTask;
    static boolean wasInBackground = true;

    public static List<Session> sessions;
    public static Map<String, Session> unsavedSessions = new HashMap<>();

    private static Application instance;
    public static LocalFS localSystem;
    public static Class loginClass, newServerClass;
    protected static Theme customTheme;

    public static RestrictionsManager restrictionsManager;

    protected static SessionCompletion onDeleteSessionListener;
    protected static SessionCompletion onEnterSession;
    protected static SessionCompletion onLogout;

    protected static Bundle applicationRestrictions;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        String oldDBFilename = baseDir().getPath() + File.separator + "database";
        DATABASE_FILE = baseDir().getPath() + File.separator + "database.sqlite";
        File dbFile = new File(oldDBFilename);

        if (dbFile.exists()) {
            boolean renamed = dbFile.renameTo(new File(DATABASE_FILE));
            if (!renamed) {
                DATABASE_FILE = oldDBFilename;
            }
        }

        String oldCacheDatabaseFilename = baseDir().getPath() + File.separator + "cache_database";
        CACHE_DATABASE_FILE = baseDir().getPath() + File.separator + "cache_database.sqlite";

        File cacheFileDBFile = new File(oldCacheDatabaseFilename);
        if (cacheFileDBFile.exists()) {
            boolean renamed = cacheFileDBFile.renameTo(new File(CACHE_DATABASE_FILE));
            if (!renamed) {
                CACHE_DATABASE_FILE = oldCacheDatabaseFilename;
            }
        }

        PERSISTENCE_KEY = getApplicationContext().getString(R.string.app_name);
        localSystem = new LocalFS(getApplicationContext());
        loginClass = UserCredentials.class;

        Accounts.init();

        Passwords.Loader = (url, login) -> {
            String id = login + "@" + (url.replace("://", "+").replace("/", "&"));
            return Database.password(id);
        };
        ClientFactory.register(new AndroidClientFactory());

        sessions = Database.listSessions();
        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            ApplicationData.name = getApplicationContext().getString(R.string.app_name);
            ApplicationData.version = pInfo.versionName;
            ApplicationData.versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            restrictionsManager = (RestrictionsManager) getApplicationContext().getSystemService(Context.RESTRICTIONS_SERVICE);
            IntentFilter restrictionsFilter;
            restrictionsFilter = new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);
            BroadcastReceiver restrictionsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (restrictionsManager != null) {
                        applicationRestrictions = restrictionsManager.getApplicationRestrictions();
                    }
                }
            };
            registerReceiver(restrictionsReceiver, restrictionsFilter);
        }

        B64.set(new B64());
    }

    //***********************************
    //          Upgrade
    //***********************************
    public static void migrate(Completion c) {
        if ("true".equals(getPreference("migrated_to_cells"))) {
            c.onComplete(null);
            return;
        }

        String server = getPreference(PREF_SERVER_URL);
        if (server != null && !"".equals(server)) {
            final ServerNode node = new ServerNode();
            Background.go(() -> {
                Error error = node.resolve(server);
                if (error != null) {
                    return;
                }

                State state = new State();

                Session session = new Session();
                session.server = node;
                session.user = getPreference(PREF_SESSION_USER);

                state.session = session.id();
                state.workspace = getPreference(PREF_WORKSPACE_ID);
                setPreference("state", state.toString());

                if (Database.saveSession(session)) {
                    unsetPreference(PREF_SERVER_URL);
                    unsetPreference(PREF_SESSION_USER);
                }

                setPreference("migrated_to_cells", "true");
                c.onComplete(null);
            });
            return;
        }
        c.onComplete(null);
    }

    //***********************************
    //          PREFERENCES
    //***********************************
    public static void unsetPreference(String key) {
        SharedPreferences sp = context().getSharedPreferences(PERSISTENCE_KEY, Activity.MODE_PRIVATE);
        SharedPreferences.Editor e = sp.edit();
        if (sp.contains(key)) {
            e.remove(key);
            e.apply();
        }
    }

    public static void setPreference(String key, String value) {
        SharedPreferences sp = context().getSharedPreferences(PERSISTENCE_KEY, Activity.MODE_PRIVATE);
        SharedPreferences.Editor e = sp.edit();
        e.putString(key, value);
        e.apply();
    }

    public static String getPreference(String key) {
        SharedPreferences sp = context().getSharedPreferences(PERSISTENCE_KEY, Activity.MODE_PRIVATE);
        String res = sp.getString(key, "");
        if (res.equals("")) {
            if (key.equals(PREF_CACHE_FOLDER))
                return Environment.getExternalStorageDirectory().getPath() + File.separator + context().getString(R.string.app_name) + File.separator + ".cache";
            if (key.equals(PREF_CACHE_SIZE))
                return "30";
            if (key.equals(PREF_CACHE_PRUNE_INTERVAL))
                return "24";

            if (key.equals(PREF_DOWNLOAD_FOLDER))
                return Environment.getExternalStorageDirectory().getPath() + File.separator + context().getString(R.string.app_name) + File.separator + "downloads";

            if (key.equals(PREF_CACHE_SIZE)) {
                return "10";
            }

            if (key.equals(PREF_CACHE_PRUNE_INTERVAL)) {
                return "2";
            }
            if (key.equals(PREF_FIRST_TIME)) {
                return "true";
            }

            if (key.equals(PREF_NETWORK_3G_PREVIEW)) {
                return "false";
            }

            if (key.equals(PREF_NETWORK_3G_TRANSFER)) {
                return "false";
            }

            if (key.equals(APPLICATION_LICENSED)) {
                return "false";
            }

            if (key.equals(PREF_PREVIOUS_SCREEN_WIDTH)) {
                return "-1";
            }

            if (key.equals(PREF_PREVIOUS_SCREEN_HEIGHT)) {
                return "-1";
            }


            if (key.equals(PREF_SAVE_PASSWORD)) {
                return "true";
            }


            if (key.equals(PREF_CLEAR_CACHE)) {
                return internalConfigs().getProperty("clear_cache", "false");
            }

            if (key.equals(PREF_SORT_ORDER)) {
                return "false";
            }

            if (key.equals(PREF_MIGRATED)) {
                return "false";
            }
        }
        return res;
    }

    public static boolean hasPref(String key) {
        SharedPreferences sp = context().getSharedPreferences(PERSISTENCE_KEY, Activity.MODE_PRIVATE);
        return sp.contains(key);
    }

    public static void clearPrefs() {
        SharedPreferences sp = context().getSharedPreferences(PERSISTENCE_KEY, Activity.MODE_PRIVATE);
        SharedPreferences.Editor e = sp.edit();
        e.clear();
        e.apply();
    }

    //***********************************
    //          Activity
    //***********************************
    public static LayoutInflater defaultInflater() {
        return (LayoutInflater) context().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static LayoutInflater inflater(Context context) {
        if (context == null) {
            return defaultInflater();
        }
        return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static void addSession(Session s) {
        if (unsavedSessions == null) {
            unsavedSessions = new HashMap<>();
        }
        unsavedSessions.put(s.id(), s);
    }

    public static void saveSession(Session s) {
        String id = s.id();
        Iterator it = sessions.iterator();
        while (it.hasNext()) {
            Session sess = (Session) it.next();
            if (id.equals(sess.id())) {
                it.remove();
                break;
            }
        }
        sessions.add(s);

        if (Database.saveSession(s)) {
            unsavedSessions.remove(id);
        } else {
            unsavedSessions.put(id, s);
        }
    }

    public static Session findSession(String id) {
        for (Session s : sessions) {
            if (id.equals(s.id())) {
                return s;
            }
        }
        return unsavedSessions.get(id);
    }

    private static void onDeleteSession(Session session) {
        onDeleteSessionListener.onComplete(session);
    }

    public static void setOnDeleteSessionListener(SessionCompletion listener) {
        onDeleteSessionListener = listener;
    }

    public static void deleteSession(int i) {
        try {
            Session session = sessions.remove(i);
            onDeleteSession(session);
        } catch (Exception ignore) {
        }
    }

    public static void onSessionLogout(Session session) {
        if (onLogout != null) {
            onLogout.onComplete(session);
        }
    }

    public static void onEnterSession(Session session) {
        if (onEnterSession != null) {
            onEnterSession.onComplete(session);
        }
    }

    public static void goForAuthenticationPage(Activity c, PydioAgent agent, boolean captcha) {
        Intent i = new Intent(c, loginClass);
        i.putExtra("agent", agent.session.id());
        i.putExtra("captcha", captcha);
        c.startActivityForResult(i, 0);
    }

    public static Context context() {
        return instance.getApplicationContext();
    }

    public static int getContentHeight() {
        String heightStr = getPreference(Application.PREF_PREVIOUS_SCREEN_HEIGHT);
        try {
            return Integer.parseInt(heightStr);
        } catch (Exception ignore) {
            Context ctx = context();
            int actionBarHeight = (int) ctx.getResources().getDimension(R.dimen.action_bar_height);
            int screenHeight = Measurement.getScreen_height(ctx);
            return screenHeight - actionBarHeight;
        }
    }

    public static int getContentWidth() {
        String widthStr = getPreference(Application.PREF_PREVIOUS_SCREEN_WIDTH);
        try {
            return Integer.parseInt(widthStr);
        } catch (Exception ignore) {
            return Measurement.getScreen_width(Application.context());
        }
    }

    //***********************************
    //          Dirs
    //***********************************
    public static File baseDir() {
        return context().getFilesDir();
    }

    public static String files() {
        return context().getFilesDir().getPath();
    }

    public static String externalDir(String type) {
        String path = Environment.getExternalStorageDirectory() + File.separator + context().getString(R.string.app_name);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    //***********************************
    //          Device
    //***********************************
    @SuppressLint("HardwareIds")
    public static String deviceID() {
        return Settings.Secure.getString(context().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String name() {
        return context().getString(R.string.app_name);
    }


    //***********************************
    //          Persistence
    //***********************************
    public static void loadSessions(Completion c) {
        Background.go(() -> {
            sessions = Database.listSessions();
            c.onComplete(null);
        });
    }

    public static void saveState(State s) {
        setPreference(PREF_APP_STATE, s.toString());
    }

    public static void clearState() {
        unsetPreference(PREF_APP_STATE);
    }

    public static State loadState() {
        String statePref = getPreference(PREF_APP_STATE);
        return State.parse(statePref);
    }

    public static int workspaceSection() {
        try {
            return Integer.parseInt(getPreference(PREF_WORKSPACE_SECTION));
        } catch (NullPointerException e) {
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void saveWorkspaceId(String id) {
        setPreference(PREF_WORKSPACE_ID, id);
    }

    public static void setSection(int section) {
        setPreference(PREF_WORKSPACE_SECTION, String.valueOf(section));
    }

    public static void setFirstTime(boolean b) {
        setPreference(PREF_FIRST_TIME, String.valueOf(b));
    }

    public static Properties internalConfigs() {
        Properties p = new Properties();
        try {
            p.load(context().getAssets().open("internalConfigs.properties"));
        } catch (IOException ignored) {
        }
        return p;
    }

    //*************************************
    //          Tasks
    //*************************************
    public static void scheduleTask(long triggerTime, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    public static void stopActivityTransitionTimer() {
        if (activityTransitionTimerTask != null) {
            activityTransitionTimerTask.cancel();
        }
        if (activityTransitionTimer != null) {
            activityTransitionTimer.cancel();
        }
        wasInBackground = false;
    }

    public static void wasInBackground() {
        boolean result = wasInBackground;
        Database.setProperty("require_pin", String.valueOf(result));
        stopActivityTransitionTimer();
    }

    public static Bundle getRestrictions() {
        return applicationRestrictions;
    }

    public static void loadTheme(String enterpriseID) {
        if (enterpriseID != null && !"".equals(enterpriseID)) {
            try {
                Theme theme = new Theme();
                Properties vanity = new Properties();
                vanity.load(new FileInputStream(baseDir() + File.separator + enterpriseID + "_vanity"));

                String mainColor = vanity.getProperty("main_tint");
                String backgroundColor = vanity.getProperty("splash_bg_color");

                applicationName = vanity.getProperty("application_name");
                applicationImage = baseDir() + File.separator + enterpriseID + "_" + "splash_image.png";

                int color = Resources.parseColor(mainColor);
                if (color != -1) {
                    theme.setMainColor(color);
                    theme.setBackgroundColor(Resources.parseColor(backgroundColor));
                    theme.setSecondaryColor(Resources.darkenColor(color));
                    theme.setSecondaryColor(Resources.oppositeColor(color));
                    customTheme = theme;
                }
            } catch (Exception ignore) {}
        }
    }

    public static Theme customTheme() {
        if (customTheme == null) {
            String enterpriseID =  getPreference(Application.ENTERPRISE_ID);
            loadTheme(enterpriseID);
        }
        return customTheme;
    }

    public static void setCustomTheme(Theme t) {
        customTheme = t;
    }
}

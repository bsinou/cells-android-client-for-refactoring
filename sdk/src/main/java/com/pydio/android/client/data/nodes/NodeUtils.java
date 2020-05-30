package com.pydio.android.client.data.nodes;

import android.content.Context;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.files.FileUtils;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.model.WorkspaceNode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NodeUtils {

    private static final String PROPERTY_OFFLINE = "offline";
    private static final String PROPERTY_OFFLINE_UNDER_ROOT = "offline_under_root";

    private static final String[] docsExtensions = new String[]{
            ".docx",
            ".doc",
            ".odt",
    };

    private static final String[] archivesExtensions = new String[]{
            ".7z",
            ".apk",
            ".jar",
            ".iso",
            ".sda",
            ".rar",
            ".tgz",
            ".tar",
            ".tar.bz2",
            ".tar.gz",
            ".tar.Z",
            ".tbz2",
            ".tar.lzma",
            ".zip"
    };

    private static final String[] videoExtensions = new String[]{
            ".webm",
            ".mkv",
            ".flv",
            ".flv",
            ".vob",
            ".ogv",
            ".ogg",
            ".drc",
            ".gif",
            ".gifv",
            ".avi",
            ".mov",
            ".qt",
            ".wmv",
            ".yuv",
            ".rm",
            ".rmvb",
            ".asf",
            ".mp4" ,
            ".m4p",
            ".m4v",
            ".mpg",
            ".mp2",
            ".mpeg",
            ".mpe",
            ".mpv",
            ".mpg",
            ".mpeg",
            ".m2v",
            ".svi",
            ".3gp",
            ".3g2",
            ".mxf",
            ".roq",
            ".nsv",
            ".wmv"
    };

    private static final String[] audioExtensions = new String[]{
            ".3gp",
            ".ac3",
            ".ai",
            ".act" ,
            ".aiff" ,
            ".aac" ,
            ".amr" ,
            ".ape" ,
            ".midi",
            ".au" ,
            ".awb" ,
            ".dct" ,
            ".dss" ,
            ".dvf" ,
            ".flac" ,
            ".gsm" ,
            ".iklax" ,
            ".ivs" ,
            ".m4a" ,
            ".m4p" ,
            ".mmf" ,
            ".mp3" ,
            ".mpc" ,
            ".msv" ,
            ".ogg" ,
            ".oga" ,
            ".opus" ,
            ".ra" ,
            ".rm" ,
            ".raw" ,
            ".sln" ,
            ".tta" ,
            ".vox" ,
            ".wav" ,
            ".wma",
            ".wv",
            ".webm",
            ".mov"
    };

    private static final String[] imagesExtensions = new String[]{
            ".jpg",
            ".jpeg",
            ".png",
            ".gif"
    };

    public static final String[] otherKnownExtensions = new String[]{
            ".html",
            ".md",
            ".php",
            ".js",
            ".java",
            ".txt"
    };

    public static int iconResource(Node node){

        if(isRecycleBin(node)){
            if("trashcan_full.png".equals(node.getProperty("icon"))) {
                return R.drawable.ic_trash_can_grey600_48dp;
            } else {
                return R.drawable.ic_trash_can_outline_grey600_48dp;
            }
        }

        try{
            if("false".equals(node.getProperty(Pydio.NODE_PROPERTY_IS_FILE))){
                return R.drawable.folder;
            }
        }catch (NullPointerException ignore){}


        return iconResource(node.label());
    }

    public static int iconResource(String label){
        Context c = Application.context();

        int iconResourceId = 0;

        String[] parts = label.split("\\.");
        if (parts.length > 1) {
            String ext = parts[parts.length - 1];
            iconResourceId = c.getResources().getIdentifier(ext, "drawable", c.getPackageName());
        }

        if(iconResourceId != 0){
            return iconResourceId;
        }

        if(isArchive(label)){
            return R.drawable.zip;
        }

        if(isAudio(label)){
            return R.drawable.audio;
        }

        if(FileUtils.isImage(label)){
            return R.drawable.image;
        }

        if(label.endsWith(".txt")){
            return R.drawable.txt;
        }

        if (label.endsWith(".pdf")){
            return R.drawable.pdf;
        }

        if (label.endsWith(".pptx") || label.endsWith(".ppt")) {
            return R.drawable.pptx;
        }

        return R.drawable.unknown_file;
    }

    public static boolean isBrowseable(Node node){
        return
                node != null &&
                        ( (node.type() == Node.TYPE_WORKSPACE || node.type() == Node.TYPE_REMOTE_NODE)
                                && ((FileNode)(node)).isFolder() || node.label().endsWith(".zip") || node.label().toLowerCase().equals("recycle bin"));
    }

    public static boolean isAudio(String label){
        for(int i = 0 ; i < audioExtensions.length; i++){
            if(label.toLowerCase().endsWith(audioExtensions[i])) return true;
        }
        return false;
    }

    public static boolean isVideo(Node node){
        for (String videoExtension : videoExtensions) {
            if (node.label().toLowerCase().endsWith(videoExtension)) return true;
        }
        return false;
    }

    public static boolean isVideo(String label){
        for (String videoExtension : videoExtensions) {
            if (label.toLowerCase().endsWith(videoExtension)) return true;
        }
        return false;
    }

    public static boolean isImage(Node node){
        if(node == null || node.type() != Node.TYPE_REMOTE_NODE) return false;

        try {
            if ("1".equals(node.getProperty(Pydio.NODE_PROPERTY_IS_IMAGE))) {
                return true;
            }
        }catch (ClassCastException ignored){}
        return isImage(node.label());
    }

    public static boolean isImage(String label){
        for (String ext : imagesExtensions) {
            if (label.toLowerCase().endsWith(ext)) return true;
        }
        return false;
    }

    public static boolean isArchive(String label){

        for(int i = 0; i < archivesExtensions.length; i++){
            if(label.endsWith(archivesExtensions[i])) return true;
        }
        return false;
    }

    public static boolean isTextFile(Node node){
        return node.label().endsWith(".txt");
    }

    public static boolean isDocument(Node node){
        String label = node.label();
        for(int i = 0; i < docsExtensions.length; i++){
            if(label.endsWith(docsExtensions[i])) return true;
        }
        return false;
    }

    public static boolean isShared(Node node){
        if(node == null) return  false;
        if(node.type() == Node.TYPE_WORKSPACE){
            String owner = ((WorkspaceNode)node).owner();
            return owner!= null && !"".equals(owner) || Pydio.WORKSPACE_ACCESS_TYPE_INBOX.equals(((WorkspaceNode)node).getAccessType());
        }
        return "true".equals(node.getProperty(Pydio.NODE_PROPERTY_AJXP_SHARED)) || "true".equals(node.getProperty("pydio_is_shared")) || node.getProperty("node_shares") != null;
    }

    public static boolean isRecycleBin(Node node){
        return "ajxp_recycle".equals(node.getProperty(Pydio.NODE_PROPERTY_AJXP_MIME)) || node.path().equals("/recycle_bin");
    }

    public static boolean isInsideRecycleBin(Node node){
        return node.path().startsWith("/recycle_bin/");
    }

    public static String parentPath(FileNode node){
        String path = node.path();
        String parent = new File(path).getParent();
        if(parent == null) return "/";
        return parent;
    }

    public static boolean hasPreview(FileNode node){
        return isImage(node) || node.label().toLowerCase().endsWith(".pdf");
    }

    public static String stringSize(double size){
        android.content.res.Resources res = Application.context().getResources();
        String[] units = res.getStringArray(R.array.size_unit);
        int i = 0;
        while (size >= 1000) {
            size = size / 1024;
            i++;
        }
        return String.format("%.0f " + units[i], size);
    }

    public static String lastModificationDate(Context c, long time){
        String message = c.getString(R.string.last_edit_time_ago);
        long elapsedMillis = new GregorianCalendar().getTimeInMillis() - time;


        long elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis);
        long elapsedMonths = elapsedDays / 31;
        long elapsedHours = TimeUnit.MILLISECONDS.toHours(elapsedMillis);
        long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis);

        if(elapsedSeconds < 60){
            message = message.replace("__TIME__", c.getString(R.string.few_seconds));
        } else if(elapsedMinutes < 60){
            message = message.replace("__TIME__", c.getString(R.string.number_of_min).replace("__NUMBER__", String.valueOf(elapsedMinutes)));
        } else if(elapsedHours < 24) {
            message = message.replace("__TIME__", c.getString(R.string.number_of_hour).replace("__NUMBER__", String.valueOf(elapsedHours) ));
        }  else if(elapsedDays < 31) {
            message = message.replace("__TIME__", c.getString(R.string.number_of_day).replace("__NUMBER__", String.valueOf(elapsedDays)));
        } else if(elapsedMonths < 5) {
            message = message.replace("__TIME__", c.getString(R.string.number_of_month).replace("__NUMBER__", String.valueOf(elapsedMonths)));
        } else {
            StringBuilder st = new StringBuilder();
            if (time != 0) {
                // DATE
                Locale locale = Locale.getDefault();
                Calendar cal = new GregorianCalendar();
                SimpleDateFormat sdf = new SimpleDateFormat(c.getString(R.string.date_format), locale);
                sdf.setCalendar(cal);
                //cal.setTime(new Date(last_modified));
                st.append(sdf.format(time));
            }
            return st.toString();
        }
        return message;
    }

    public static boolean isOffline(Node node){
        return "true".equals(node.getProperty(NodeUtils.PROPERTY_OFFLINE));
    }

    public static boolean isBookmarked(Node node) {
        return "true".equals(node.getProperty(Pydio.NODE_PROPERTY_BOOKMARK)) || "true".equals(node.getProperty(Pydio.NODE_PROPERTY_AJXP_BOOKMARKED));
    }

    public static boolean isUnderOfflineFolder(Node node){
        return "true".equals(node.getProperty(NodeUtils.PROPERTY_OFFLINE_UNDER_ROOT));
    }

    public static boolean isFolder(Node node) {
        int type = node.type();
        if(type == Node.TYPE_LOCAL_NODE){
            return ((FileNode)node).isFolder();
        } else if (type == Node.TYPE_REMOTE_NODE){
            return ((FileNode)node).isFolder();
        }
        return false;
    }

    public static long lastModified(Node node){
        if( node.type() == Node.TYPE_WORKSPACE) {
            return 0;
        }
        String mtime = node.getProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME);
        if (mtime == null) {
            return 0;
        }
        return Long.parseLong(mtime);
    }

    public static long size(Node node){
        if( node.type() == Node.TYPE_WORKSPACE) {
            return 0;
        }
        String byteSize = node.getProperty(Pydio.NODE_PROPERTY_BYTESIZE);
        if (byteSize != null) {
            return Long.parseLong(byteSize);
        }

        return 0;
    }
}


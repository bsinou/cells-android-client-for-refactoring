package com.pydio.android.client.data;

import android.os.Environment;

import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.common.codec.Hex;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.ServerNode;
import com.pydio.sdk.core.model.WorkspaceNode;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class Session implements Serializable {

    private String ID;
    public ServerNode server;
    public String user;

    public String idForCredentials() {
        String serverID = server.url().replace("://", "+").replace("/", "&");
        return String.format("%s@%s", user, serverID);
    }

    public String id() {
        if (ID == null || "".equals(ID)) {
            String id = String.format("%s@%s", user, server.url());
            ID = Hex.toString(id.getBytes(Charset.forName("UTF-8")));
        }
        return ID;
    }

    public String tokenKey() {
        return String.format("%s@%s", user, server.url());
    }

    public String workspaceCacheID (String workspace) {
        return String.format("%s.%s", id(), workspace);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof Session)) return false;
        Session b = (Session) o;

        if (this.user != null && !this.user.equals(b.user)) {
            return false;
        }

        URL u = null, ou;

        try {
            u = new URL(this.server.url());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            ou = new URL(b.server.url());
        } catch (MalformedURLException e) {
            return false;
        }

        return u != null &&
                ou.getHost().equals(u.getHost()) &&
                ou.getPath().equals(u.getPath()) &&
                ou.getProtocol().equals(u.getProtocol()) &&
                ou.getPort() == u.getPort();
    }

    public WorkspaceNode resolveNodeWorkspace(FileNode node) {
        String slug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
        if (slug != null && !"".equals(slug)) {
            return server.getWorkspace(slug);
        }

        String id = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_ID);
        if (id == null || "".equals(id)) {
            return null;
        }

        return server.findWorkspaceById(id);
    }

    //********************************************************************************
    //                  DIRS
    //********************************************************************************
    public String externalBaseFolderPath() {
        String path = Application.externalDir(null) + File.separator + user + File.separator + server.url();
        File f = new File(path);
        if(!f.exists()) {
            boolean result = f.mkdirs();
            if (!result) {
                return null;
            }
        }
        return f.getPath();
    }

    public String publicDownloadPath(String label) {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        String extension = "";
        String baseName = label;
        int i = label.lastIndexOf('.');
        if (i > 0) {
            extension = "." + label.substring(i+1);
            baseName = label.substring(0, i);
        }

        i = 0;
        File file = new File(downloadDir, label);
        while(file.exists()) {
            i++;
            file = new File(downloadDir, baseName + "-" + i + extension);
        }
        return file.getPath();
    }

    public String externalTempDir(){
        String path = externalBaseFolderPath() + File.separator +  Application.TEMP_FOLDER;
        File file = new File(path);
        if(!file.exists() && !file.mkdirs()){
            return null;
        }
        return path;
    }

    public String baseFolderPath() {
        String path = Application.baseDir().getPath() + File.separator + id();
        File f = new File(path);
        if(!f.exists()){
            boolean result = f.mkdirs();
            if (!result) {
                return null;
            }
        }
        return f.getPath();
    }

    public String workspacePath(String ws){
        final String sep = File.separator;
        String wsPath = baseFolderPath() + sep + ws;

        File file = new File(wsPath);
        if(!file.exists() && !file.mkdirs()){
            return null;
        }
        return wsPath;
    }

    public String downloadPath(String ws, String f) {
        final String sep = File.separator;
        String downloadPath = baseFolderPath() + sep + ws + sep + f;
        File file = new File(downloadPath);
        File parent = file.getParentFile();
        if(!parent.exists() && !file.getParentFile().mkdirs()){
            return null;
        }
        return downloadPath;
    }

    public String cacheFolderPath() {
        String path = baseFolderPath() + File.separator + ".cache";
        File file = new File(path);
        if(!file.exists() && !file.mkdirs()){
            return null;
        }
        return path;
    }

    public String tempFolderPath() {
        String path = baseFolderPath() + File.separator +  ".temp";
        File file = new File(path);
        if(!file.exists() && !file.mkdirs()){
            return null;
        }
        return path;
    }

    public String offlineTaskFolder(String ws){
        final String sep = File.separator;
        String offlineFolderPath = baseFolderPath() + sep + ws + sep + ".offline";
        File file = new File(offlineFolderPath);
        if(!file.exists() && !file.mkdirs()) {
            return null;
        }
        return offlineFolderPath;
    }

}

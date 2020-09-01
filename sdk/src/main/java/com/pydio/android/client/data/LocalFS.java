package com.pydio.android.client.data;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;

import com.pydio.android.client.data.callback.Completion;
import com.pydio.android.client.data.files.FileUtils;
import com.pydio.android.client.data.nodes.NodeUtils;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.common.callback.NodeHandler;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.model.NodeFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class LocalFS {

    protected String default_root;
    public static final long MEGA_BYTE = 1048576;

    Context c;

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public void registerToDeviceScanner(File file) {
        MediaScannerConnection.scanFile(Application.context(), new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        // Log.i("ExternalStorage", "Scanned " + path + ":");
                        // Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    public LocalFS(Context c) {
        this.c = c;
        File file = Environment.getExternalStorageDirectory();
        default_root = file.getAbsolutePath();
    }

    public void setRoot(String root) {
        default_root = root;
    }

    public void listParent(Node node, NodeHandler handler) {
        String path = node == null ? default_root : node.path();
        File file = new File(path);
        file = new File(file.getParent());
        if (!file.isDirectory())
            return;

        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        for (int i = 0; i < children.length; i++) {
            handler.onNode(NodeFactory.createNode(Node.TYPE_REMOTE_NODE, children[i]));
        }
    }

    public long[] scanFolder(String path) {
        File f = new File(path);
        long[] result = new long[] { 1, f.length() };
        if (f.isFile()) {
            return result;
        }

        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.getName().startsWith(".")) {
                long[] r = scanFolder(file.getPath());
                result[0] += r[0];
                result[1] += r[1];
            }
        }
        return result;
    }

    public void scanImages(Context c, Completion<String> onNewFile) {
        Cursor cursor;
        int column_index_data;
        String[] projection = { MediaStore.MediaColumns.DATA };
        cursor = c.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                null);
        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                onNewFile.onComplete(cursor.getString(column_index_data));
            }
            cursor.close();
        }
    }

    public void scanVideos(Context c, Completion<String> onNewFile) {
        Cursor cursor;
        int column_index_data;
        String[] projection = { MediaStore.MediaColumns.DATA };
        cursor = c.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                null);
        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                onNewFile.onComplete(cursor.getString(column_index_data));
            }
            cursor.close();
        }
    }

    public void scanAudios(Context c, Completion<String> onNewFile) {
        Cursor cursor;
        int column_index_data;
        String[] projection = { MediaStore.MediaColumns.DATA };
        cursor = c.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                null);
        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                onNewFile.onComplete(cursor.getString(column_index_data));
            }
            cursor.close();
        }
    }

    public void scanFiles(String folder, Completion<String> onNewFile) {
        File fileFolder = new File(folder);
        String[] list = fileFolder.list();

        for (int i = 0; i < list.length; i++) {
            String path = folder + File.separator + list[i];
            onNewFile.onComplete(path);
            if (new File(path).isDirectory()) {
                scanFiles(path, onNewFile);
            }
        }
    }

    public synchronized void list(Node node, NodeHandler handler) {
        String path = node == null ? default_root : node.path();
        File file = new File(path);
        if (!file.isDirectory())
            return;
        File[] children = file.listFiles();
        for (int i = 0; i < children.length; i++) {
            handler.onNode(NodeFactory.createNode(Node.TYPE_REMOTE_NODE, children[i]));
        }
    }

    public OutputStream outputStreamFromPath(String path) throws IOException {
        try {
            return new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            File file = new File(path);
            if (!file.exists() && !new File(file.getParent()).mkdirs())
                throw e;
            return new FileOutputStream(file);
        }
    }

    public File folder(String path) {
        File file = new File(path);
        file.setWritable(true, true);
        file.setReadable(true);

        if (file.exists() && !file.isDirectory())
            return null;

        if (file.exists()) {
            return file;
        }

        if (file.mkdirs()) {
            return file;
        }
        return null;
    }

    public File createFile(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public InputStream getPreviewData(int dimension) {
        return null;
    }

    public static String md5(String path) {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");

            FileInputStream fis = new FileInputStream(path);

            byte[] dataBytes = new byte[1024];

            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            byte[] mdbytes = md.digest();

            // convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            // convert the byte to hex format method 2
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
                String hex = Integer.toHexString(0xff & mdbytes[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ignore) {
            // Log.e("Runtime", e.getMessage());
        } catch (IOException ignore) {
            // Log.e("IO", e.getMessage());
        } catch (Exception ignore) {

        }
        return "";
    }

    public boolean mkdir(String path) {
        return new File(path).mkdirs();
    }

    public boolean mkfile(String path) {
        try {
            return new File(path).createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    public StatFs stat(Node node) {
        return new StatFs(node.path());
    }

    public boolean delete(String path) {
        File f = new File(path);
        if (f.isDirectory()) {
            while (f.list() != null && f.list().length > 0 && !deleteDirectory(new File(path)))
                ;
            return true;
        } else {
            return !f.exists() || f.delete();
        }
    }

    public boolean deleteDirectory(File f) {
        boolean result = true;
        if (f.isDirectory()) {
            String[] list = f.list();
            for (int i = 0; list != null && i < f.list().length; i++) {
                File child = new File(f.getPath() + File.separator + list[i]);
                if (child.isDirectory()) {
                    result &= deleteDirectory(child);
                }
                result &= child.delete();
            }
        }
        return result && f.delete();
    }

    public void deleteAll(String path, String mime) {
        File file = new File(path);
        if (file.isDirectory()) {
            String[] list = file.list();
            for (int i = 0; i < list.length; i++) {
                String m = FileUtils.mimeType(list[i]);
                if (m.startsWith(mime)) {
                    delete(path + File.separator + list[i]);
                }
            }
        }
    }

    public boolean move(File source, File target) {
        return source.renameTo(target);
    }

    public boolean move(String source, String target) {
        return new File(source).renameTo(new File(target));
    }

    public void moveTo(File src, File dst) {
        if (src == null)
            return;
        String subDst = dst.getPath() + File.separator + src.getName();

        if (src.isDirectory()) {
            mkdir(subDst);
            String[] children = src.list();
            for (String child : children) {
                moveTo(new File(src, child), new File(subDst));
            }
            return;
        }
        move(src.getPath(), subDst);
    }

    public void copy(File src, File tgt) throws IOException {
        byte[] buffer = new byte[Pydio.LOCAL_CONFIG_BUFFER_SIZE_DEFAULT_VALUE];
        InputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(tgt);
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    public void copy(String src, String tgt) throws IOException {
        byte[] buffer = new byte[Pydio.LOCAL_CONFIG_BUFFER_SIZE_DEFAULT_VALUE];
        InputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(tgt);
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    public long size(String path) {
        return new File(path).length();
    }

    public ArrayList<File> search(String root, String expression) {
        ArrayList<File> result = new ArrayList<File>();
        File file = new File(root);
        String[] children = file.list();

        for (int i = 0; i < children.length; i++) {
            String childPath = root + File.separator + children[i];
            File childFile = new File(childPath);
            if (childFile.isDirectory()) {
                result.addAll(search(childPath, expression));
            }

            if (children[i].contains(expression)) {
                result.add(childFile);
            }
        }
        return result;
    }

    public static long folderSize(String path) {
        long length = 0;
        File f = new File(path);
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File file : f.listFiles()) {
                    if (file.isFile())
                        length += file.length();
                    else
                        length += folderSize(file.getPath());
                }
            } else {
                length = f.length();
            }
        }
        return length;
    }

    public void unzip(String zipPath, String output, Completion<File> c) throws IOException {
        FileInputStream fin = new FileInputStream(zipPath);
        ZipInputStream zin = new ZipInputStream(fin);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            // Log.v("Decompress", "Unzipping " + ze.getName());

            String path = output + File.separator + ze.getName();
            if (ze.isDirectory()) {
                folder(path);
            } else {
                byte[] buffer = new byte[16384];
                FileOutputStream fout = new FileOutputStream(path);
                int read;
                while ((read = zin.read(buffer)) != -1) {
                    fout.write(buffer, 0, read);
                }
                zin.closeEntry();
                fout.close();
                if (c != null) {
                    c.onComplete(new File(path));
                }
            }
        }
        zin.close();
    }

    public void zip(String[] files, String output) throws IOException {
        BufferedInputStream origin = null;
        FileOutputStream dest = new FileOutputStream(output);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
        byte[] data = new byte[16384];
        for (int i = 0; i < files.length; i++) {
            // Log.v("Compress", "Adding: " + files[i]);
            FileInputStream fi = new FileInputStream(files[i]);
            origin = new BufferedInputStream(fi, 16384);
            ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, 16384)) != -1) {
                out.write(data, 0, count);
            }
            origin.close();
        }
        out.close();
    }

    public long totalSpace(boolean external) {
        StatFs statFs = getStats(external);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return (((statFs.getBlockCountLong()) * (statFs.getBlockSizeLong())));
        }
        return ((((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize())));
    }

    /**
     * Calculates free space on disk
     * 
     * @param external If true will query external disk, otherwise will query
     *                 internal disk.
     * @return Number of free mega bytes on disk.
     */
    public long freeSpace(boolean external) {
        StatFs statFs = getStats(external);
        long availableBlocks = 0, blockSize = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            availableBlocks = statFs.getAvailableBlocksLong();
            blockSize = statFs.getBlockSizeLong();
        } else {
            availableBlocks = statFs.getAvailableBlocks();
            blockSize = statFs.getBlockSize();
        }
        long freeBytes = availableBlocks * blockSize;

        return freeBytes;
    }

    /**
     * Calculates occupied space on disk
     * 
     * @param external If true will query external disk, otherwise will query
     *                 internal disk.
     * @return Number of occupied mega bytes on disk.
     */
    public long busySpace(boolean external) {
        return totalSpace(external) - freeSpace(external);
    }

    private StatFs getStats(boolean external) {
        String path;
        if (external) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            path = Environment.getRootDirectory().getAbsolutePath();
        }
        return new StatFs(path);
    }

    private static String hexify(byte[] bytes) {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

        StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }

        return buf.toString();
    }

    public static FileNode fileToNode(File file) {
        FileNode result = new FileNode();
        boolean isFile = !file.isDirectory();
        boolean isImage = NodeUtils.isImage(file.getName());
        // result.setProperty(Pydio.NODE_PROPERTY_UUID, node.getUuid());
        result.setProperty(Pydio.NODE_PROPERTY_TEXT, file.getName());
        result.setProperty(Pydio.NODE_PROPERTY_LABEL, file.getName());
        result.setProperty(Pydio.NODE_PROPERTY_BYTESIZE, "" + file.length());
        result.setProperty(Pydio.NODE_PROPERTY_PATH, file.getPath());
        result.setProperty(Pydio.NODE_PROPERTY_FILENAME, file.getPath());
        result.setProperty(Pydio.NODE_PROPERTY_IS_FILE, String.valueOf(isFile));
        result.setProperty(Pydio.NODE_PROPERTY_IS_IMAGE, String.valueOf(isImage));
        // result.setProperty(Pydio.NODE_PROPERTY_FILE_PERMS,
        // String.valueOf(node.getMode()));
        result.setProperty(Pydio.NODE_PROPERTY_AJXP_MODIFTIME, String.valueOf(file.lastModified()));
        return result;
    }
}

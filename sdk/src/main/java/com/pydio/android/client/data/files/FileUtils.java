package com.pydio.android.client.data.files;

import android.graphics.BitmapFactory;

import java.io.File;

public class FileUtils {

    public static boolean isImage(File file){
        String mime = getMimeType(file.getPath().toLowerCase());
        String label = file.getName();
        int index = label.lastIndexOf('.');
        String extension;
        if(index == -1){
            return false;
        }else{
            extension = label.substring(index).toLowerCase();
        }
        return mime != null && mime.toLowerCase().contains("image") || ".png".equals(extension) || ".jpg".equals(extension) || ".jpeg".equals(extension) || ".gif".equals(extension);
    }

    public static boolean isImage(String label){
        int index = label.lastIndexOf('.');
        String extension;
        if(index == -1){
            return false;
        }else{
            extension = label.substring(index).toLowerCase();
        }
        return ".png".equals(extension) || ".jpg".equals(extension) || ".jpeg".equals(extension) || ".gif".equals(extension);
    }

    public static int[] imageDimensions(File file){
        //thanks to Devunwired on stackOverflow
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        //Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(file.getPath(), options);
        int width = options.outWidth;
        int height = options.outHeight;
        //If you want, the MIME type will also be decoded (if possible)
        //String type = options.outMimeType;
        return new int[]{width, height};
    }

    public static String getMimeType(String url) {
        return mimeType(url);
    }

    public static String mimeType(String label){
        int index = label.lastIndexOf('.');
        if(index == -1 || label.endsWith(".")){
            return "text/plain";
        }
        return MimeUtils.guessMimeTypeFromExtension(label.substring(index + 1).toLowerCase());
    }

    public static String nodeTempName(String label, long lastModified){
        int index = label.lastIndexOf(".");
        String name= label, ext = "";
        if(index != -1){
            name = label.substring(0, index-1);
            ext = label.substring(index-1);
        }
        return name + "_" + lastModified + ext;
    }
}

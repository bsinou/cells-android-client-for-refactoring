package com.pydio.android.client.data.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.opengl.GLES10;
import android.os.Debug;

import com.pydio.sdk.core.utils.io;

import java.io.FileOutputStream;



public class ImageBitmap {

    private static int OPEN_GL_MAX_TEXTURE_SIZE = 0;

    //*****************************************************************************************
    //                  UTILS
    //*****************************************************************************************
    public static void storeBitmap(Bitmap b, String path){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            //Log.e("unspecific", e.getMessage());
        } finally {
            io.close(out);
        }
    }

    public static Bitmap loadBitmap(String path, int w, int h, boolean square) {
        float scale = ((float)w) / h;

        if(getOpenGlMaxTextureSize() > 0) {
            w = h = Math.min(Math.min(w, h), getOpenGlMaxTextureSize());
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        if(!checkBitmapFitsInMemory(options.outWidth, options.outHeight, Math.max(options.inTargetDensity, Math.max(options.inScreenDensity, options.inDensity)))){
            if(w == 0 || h == 0) return null;
            return loadBitmap(path, w/2, square? w/2 : (int)(w/(2*scale)), square);
        }

        options.inSampleSize = calculateSampleSize(options, w, h);
        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        if(!square){
            return bm;
        }
        return bm;
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;

        if (width > reqWidth || height > reqHeight) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public static float[] dimensions(String path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int width = options.outWidth;
        int height = options.outHeight;
        return new float[]{width, height};
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        if(newWidth >= width || newHeight >= height){
            return bm;
        }

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private static boolean checkBitmapFitsInMemory(long bmpwidth, long bmpheight, int bmpdensity){
        long reqsize=bmpwidth*bmpheight*bmpdensity;
        long allocNativeHeap = Debug.getNativeHeapAllocatedSize();

        final long heapPad=(long) Math.max(4*1024*1024,Runtime.getRuntime().maxMemory()*0.1);
        return (reqsize + allocNativeHeap + heapPad) < Runtime.getRuntime().maxMemory();
    }

    private static int getOpenGlMaxTextureSize(){
        if(OPEN_GL_MAX_TEXTURE_SIZE > 0) return OPEN_GL_MAX_TEXTURE_SIZE;
        int[] maxTextureSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        return OPEN_GL_MAX_TEXTURE_SIZE = maxTextureSize[0];
    }
}

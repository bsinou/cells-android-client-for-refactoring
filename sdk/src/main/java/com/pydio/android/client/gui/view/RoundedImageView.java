package com.pydio.android.client.gui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class RoundedImageView extends AppCompatImageView {

    private int colorFilter = 0;
    Runnable mDrawRunnable = () -> {
        try {
            draw(null);
        }catch (Exception e){
            //often NullPointerException is thrown when Canvas.isHardwareAccelerated is called
            //not bad you will only drawable a blank rounded surface on left panel for a moment
        }
    };

    Canvas mCanvas;

    public RoundedImageView(Context context) {
        super(context);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(canvas == null){
            canvas = mCanvas;
        }

        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        Bitmap b = ((BitmapDrawable) drawable).getBitmap();
        Bitmap bitmap;
        try {
            bitmap = b.copy(Bitmap.Config.ARGB_8888, true);
            int w = getWidth();
            Bitmap roundBitmap = getCroppedBitmap(bitmap, w);
            canvas.drawBitmap(roundBitmap, 0, 0, null);
        } catch(OutOfMemoryError e){
            mCanvas = canvas;
            postDelayed(mDrawRunnable, 500);
        } catch (Exception e){
            mCanvas = canvas;
            postDelayed(mDrawRunnable, 500);
        }
    }

    public Bitmap getCroppedBitmap(Bitmap bmp, int radius) {
        Bitmap sbmp;

        if (bmp.getWidth() != radius || bmp.getHeight() != radius) {
            float smallest = Math.min(bmp.getWidth(), bmp.getHeight());
            float factor = smallest / radius;
            sbmp = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth() / factor), (int)(bmp.getHeight() / factor), false);
        } else {
            sbmp = bmp;
        }

        Bitmap output = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, radius, radius);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);

        ColorFilter filter = this.getColorFilter();
        if(filter != null){
            //paint.setColor(Color.parseColor("#00ffffff"));
            paint.setColorFilter(filter);
        } else {
            paint.setColor(Color.parseColor("#ffffff"));
        }
        canvas.drawCircle(radius / 2 + 0.7f, radius / 2 + 0.7f, radius / 2 + 0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        paint.setColorFilter(filter);

        canvas.drawBitmap(sbmp, rect, rect, paint);

        return output;
    }

    public void setFilter(int res){
        colorFilter = res;
    }
}
package com.pydio.android.client.data.images;

import android.graphics.Bitmap;

import com.pydio.android.client.data.callback.Completion;
import com.pydio.sdk.core.model.Node;

public interface ThumbLoader {
    void loadThumb(Node node, int dimen, Completion<Bitmap> bitmapCompletion);
}

package com.pydio.android.client.data.encoding;
import android.os.Build;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class B64 extends com.pydio.sdk.core.encoding.B64 {


    @Override
    public byte[] decode(byte[] data) {
        return Base64.decode(data, Base64.DEFAULT|Base64.NO_WRAP|Base64.NO_PADDING|Base64.NO_CLOSE);
    }

    public String decodeToString(byte[] data) {
        byte[] result = this.decode(data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new String(result, StandardCharsets.UTF_8);

        } else {
            try {
                return new String(result, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return new String(result);
        }
    }

    @Override
    public byte[] encode(byte[] data) {
        return Base64.encode(data, Base64.DEFAULT|Base64.NO_WRAP|Base64.NO_PADDING|Base64.NO_CLOSE);
    }

    @Override
    public String decode(String s) {
        byte[] data = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            data = s.getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                data = s.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                data = s.getBytes();
            }
        }

        return decodeToString(data);
    }

    @Override
    public String encode(String s) {
        byte[] data = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            data = s.getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                data = s.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                data = s.getBytes();
            }
        }
        return encodeToString(data);
    }

    public String encodeToString(byte[] data) {
        byte[] result = this.encode(data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new String(result, StandardCharsets.UTF_8);

        } else {
            try {
                return new String(result, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return new String(result);
        }
    }
}

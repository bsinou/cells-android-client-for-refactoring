package com.pydio.android.client.utils;

public class Threading {
    public static void sleep(long interval) {
        try {
            Thread.sleep(interval);
        } catch (Exception ignored) {
        }
    }
}

package com.pydio.android.client.data;

public class Display {

    public static final int list = 1;
    public static final int grid = 2;
    public static final int section = 3;
    public static final int entitledSection = 3;

    public interface Info {
        int mode();
    }
}

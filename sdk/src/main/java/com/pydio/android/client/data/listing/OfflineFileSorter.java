package com.pydio.android.client.data.listing;

import java.io.File;

public class OfflineFileSorter implements Sorter<File> {

    @Override
    public boolean isBefore(File n1, File n2) {
        boolean result =
        n1.isDirectory() && !n2.isDirectory() ||
        (n1.isDirectory() && n2.isDirectory() || !n1.isDirectory() && !n2.isDirectory()) && n1.getName().toLowerCase().compareToIgnoreCase(n2.getName().toLowerCase()) < 0;
        return result;
    }
}

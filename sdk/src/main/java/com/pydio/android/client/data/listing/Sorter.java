package com.pydio.android.client.data.listing;

public interface Sorter<T> {
    boolean isBefore(T n1, T n2);
}

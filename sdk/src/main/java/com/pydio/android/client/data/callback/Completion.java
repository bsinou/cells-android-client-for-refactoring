package com.pydio.android.client.data.callback;

public interface Completion<T> {
    void onComplete(T t);
}

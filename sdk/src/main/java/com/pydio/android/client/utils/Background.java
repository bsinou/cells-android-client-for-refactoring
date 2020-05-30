package com.pydio.android.client.utils;

public class Background {
    private Runnable runnable;

    private Background(Runnable r) {
        this.runnable = r;
    }

    public static Task go(Runnable r) {
        return new Background(r).execute();
    }

    public Task execute() {
        Task tracker = new Task();
        Thread t = new Thread(() -> {
            runnable.run();
            tracker.setDone();
        });
        tracker.setTask(t);
        t.start();
        return tracker;
    }
}

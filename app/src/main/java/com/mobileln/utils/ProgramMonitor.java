package com.mobileln.utils;

import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ProgramMonitor {

    private static final int CHECK_INTERVAL_MS = 1000;
    private volatile boolean mIsRunning = false;
    private ConcurrentLinkedQueue<Runnable> mJobsQueue = new ConcurrentLinkedQueue<>();

    public synchronized boolean startMonitor() {
        if (mIsRunning) {
            Log.i(this.getClass().getName(), "It's already running, cannot startMonitor()");
            return false;
        }
        new Thread() {
            @Override
            public void run() {
                Log.i(this.getName(), "Monitor starts");
                // TODO: Set priority?
                while(mIsRunning) {
                    Runnable job = mJobsQueue.poll();
                    if (job != null) {
                        job.run();
                    } else {
                        // TODO: wait when app is in background, resume when it's in foreground?
                        SystemClock.sleep(CHECK_INTERVAL_MS);
                        defaultJob();
                    }
                }
                Log.i(this.getName(), "Monitor stopped");
            }
        }.start();
        mIsRunning = true;
        return true;
    }

    public abstract void defaultJob();

    public void addJob(Runnable runnable) {
        mJobsQueue.add(runnable);
    }

    public synchronized boolean stopMonitor() {
        if (!mIsRunning) {
            Log.i(this.getClass().getName(), "It'not running, cannot stopMonitor()");
            return false;
        }
        mIsRunning = false;
        return true;
    }

    public synchronized boolean isRunning() {
        return mIsRunning;
    }
}

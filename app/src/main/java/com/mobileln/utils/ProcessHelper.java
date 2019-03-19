package com.mobileln.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

public class ProcessHelper {

    private static final String TAG = "ProcessHelper";

    private String[] mExecutableAndArgs = null;
    private Thread mRunningThread = null;
    private Process mProcess = null;
    private CircularArray mCircularArray;
    private boolean mRedirectError = false;

    private StringBuffer mLogStringBuffer;
    // TODO: Better way to notify when it's done
    private CountDownLatch mLogStringDone = null;

    public ProcessHelper(int size, boolean redirectError) {
        mCircularArray = new CircularArray(size);
        mLogStringBuffer = null;
        mRedirectError = redirectError;
    }

    public ProcessHelper(boolean redirectError) {
        mCircularArray = null;
        mLogStringBuffer = new StringBuffer();
        mRedirectError = redirectError;
    }

    public ProcessHelper() {
        mCircularArray = null;
        mLogStringBuffer = new StringBuffer();
    }

    protected synchronized void setExecutableAndArgs(String... executableAndArgs) {
        mExecutableAndArgs = executableAndArgs;
    }

    protected synchronized void startProcess(Context context, final String tag) throws IOException {
        if (mRunningThread != null || mProcess != null) {
            Log.e(tag, "Process is running, cannot start process");
            return;
        }
        if (mExecutableAndArgs == null) {
            Log.e(tag, "Empty args, cannot start process");
            return;
        }
        ProcessBuilder builder = new ProcessBuilder(mExecutableAndArgs);
        // TODO: Set it properly
        builder.environment().put("HOME", FileUtils.getBitcoindExecutable(context));
        // TODO: Fix dirty code
        if (mRedirectError) {
            builder.redirectErrorStream(true);
        }
        mProcess = builder.start();
        final Process process = mProcess;
        mLogStringDone = new CountDownLatch(1);
        mRunningThread = new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "Start processing: " + tag);
                try {
                    try (InputStream inputStream = process.getInputStream()) {
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(inputStream));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.equals("\n") || TextUtils.isEmpty(line)) {
                                continue;
                            }
                            if (mCircularArray != null) {
                                mCircularArray.add(line);
                            } else {
                                mLogStringBuffer.append(line);
                                mLogStringBuffer.append('\n');
                            }
                        }
                        Log.e(TAG, "No more lines?");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } finally {
                    Log.e(TAG, "Finish processing: " + tag);
                    mLogStringDone.countDown();
                }
            }
        };
        new Thread() {
            public void run() {
                try {
                    mProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mProcess = null;
                mRunningThread = null;
            }
        }.start();
        // TODO: Fix dirty code
        if (mCircularArray != null) {
            new Thread() {
                @Override
                public void run() {
                    try (InputStream inputStream = mProcess.getErrorStream()){
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        while (bufferedReader.readLine() != null) {
                        }
                    } catch (IOException e) {
                    }
                }
            }.start();
        }
        mRunningThread.start();
    }

    protected synchronized void stopProcess() {
        if (mRunningThread == null) {
            Log.e(TAG, "mRunningThread is null, skip stop");
        }
        if (mProcess != null) {
            mProcess.destroy();
            Log.e(TAG, "DESTROY JOR!!!!");
        }
    }

    public synchronized boolean isRunning() {
        return mRunningThread != null;
    }

    protected synchronized void waitFor() throws InterruptedException {
        if (mLogStringDone != null && mLogStringBuffer != null) {
            mLogStringDone.await();
        }
    }

    protected synchronized void clearOutput() {
        mLogStringBuffer = new StringBuffer();
    }

    public String getOutput() {
        if (mCircularArray != null) {
            return mCircularArray.get();
        } else {
            return mLogStringBuffer.toString();
        }
    }
}

package com.mobileln.lightningd;

import android.content.Context;

import org.json.JSONException;

import java.io.IOException;

import com.mobileln.MyApplication;
import com.mobileln.utils.ProgramMonitor;

public class LightningdState extends ProgramMonitor {

    private static final LightningdState INSTANCE = new LightningdState();

    public static final int STATE_DISCONNECTED = -1;
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_NOT_RUNNING = 1;
    public static final int STATE_STARTING = 2;
    public static final int STATE_SYNCING = 3;
    public static final int STATE_READY = 4;

    private int mCurrentState = 0;
    private double mBlockHeight = 0;

    public static LightningdState getInstance() {
        return INSTANCE;
    }

    @Override
    public void defaultJob() {
        addJob(mUpdateBlockHeight);
    }

    private Runnable mUpdateBlockHeight = new Runnable() {
        @Override
        public void run() {
            Context context = MyApplication.getContext();
            try {
                mBlockHeight = LightningCli.newInstance().getBlockHeight(context);
            } catch (IOException | JSONException e) { }
            if (mBlockHeight > 0) {
                mCurrentState = STATE_READY;
            } else {
                mCurrentState = STATE_SYNCING;
            }
        }
    };

    public int getCurrentState() {
        return mCurrentState;
    }


    @Override
    public boolean stopMonitor() {
        mCurrentState = STATE_NOT_RUNNING;
        return super.stopMonitor();
    }
}

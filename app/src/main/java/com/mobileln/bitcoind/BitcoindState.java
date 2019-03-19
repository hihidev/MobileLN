package com.mobileln.bitcoind;

import com.mobileln.utils.ProgramMonitor;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class BitcoindState extends ProgramMonitor {

    private static final BitcoindState INSTANCE = new BitcoindState();

    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_NOT_RUNNING = 1;
    public static final int STATE_STARTING = 2;
    public static final int STATE_SYNCING = 3;
    public static final int STATE_READY = 4;

    private volatile int mCurrentState = STATE_NOT_RUNNING;
    private double mVerificationProgress = 0;

    public static BitcoindState getInstance() {
        return INSTANCE;
    }

    @Override
    public void defaultJob() {
        updateVerificationProgress();
    }

    private Runnable mUpdateVerificationProgress = new Runnable() {
        @Override
        public void run() {
            if (!Bitcoind.getInstance().isRunning()) {
                mCurrentState = STATE_NOT_RUNNING;
                return;
            }
            if (mCurrentState == STATE_NOT_RUNNING) {
                mCurrentState = STATE_STARTING;
            }
            try {
                mVerificationProgress = BitcoinCli.getVerificationProgress();
                if (mVerificationProgress > 0.99) {
                    mCurrentState = STATE_READY;
                } else {
                    mCurrentState = STATE_SYNCING;
                }
            } catch (BitcoinRPCException | UnsupportedOperationException e) {
                e.printStackTrace();
            }
        }
    };

    public void updateVerificationProgress() {
        addJob(mUpdateVerificationProgress);
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    @Override
    public boolean stopMonitor() {
        // TODO: Race condition?
        mCurrentState = STATE_NOT_RUNNING;
        return super.stopMonitor();
    }

    public double getVerificationProgress() {
        return mVerificationProgress;
    }
}

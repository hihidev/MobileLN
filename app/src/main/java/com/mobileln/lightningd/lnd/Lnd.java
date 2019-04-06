package com.mobileln.lightningd.lnd;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.WorkerThread;

import com.mobileln.bitcoind.Bitcoind;
import com.mobileln.lightningd.LightningdState;
import com.mobileln.utils.FileUtils;
import com.mobileln.utils.ProcessHelper;

import java.io.IOException;
import java.util.ArrayList;

public class Lnd extends ProcessHelper {
    private static final String TAG = "Lnd";
    private static final int BUFFER_SIZE = 100;
    private static final int START_SERVICE_MAX_RETRY = 10;
    private static final int START_SERVICE_RETRY_INTERVAL_MS = 300;
    private static final Lnd
            INSTANCE = new Lnd();

    private Lnd() {
        super(BUFFER_SIZE, true);
    }

    public static Lnd getInstance() {
        return INSTANCE;
    }

    @WorkerThread
    public synchronized void startService(Context context) throws IOException {
        for (int i = 0; i < START_SERVICE_MAX_RETRY && isRunning(); i++) {
            SystemClock.sleep(START_SERVICE_RETRY_INTERVAL_MS);
        }
        final String executable = FileUtils.getLndExecutable(context);
        final String lndDataDir = FileUtils.getLndDataFolderPath(context);
        final String lndConfigFile = FileUtils.getLndConfigFilePath(context);

        ArrayList<String> executableAndArgs = new ArrayList<>();
        executableAndArgs.add(executable);
        executableAndArgs.add("--lnddir=" + lndDataDir);
        executableAndArgs.add("--configfile=" + lndConfigFile);
        executableAndArgs.add("--bitcoind.rpcuser=" + Bitcoind.getInstance().getRpcUserName());
        executableAndArgs.add("--bitcoind.rpcpass=" + Bitcoind.getInstance().getRpcPassowrd());
        executableAndArgs.add("--bitcoind.rpcpass=" + Bitcoind.getInstance().getRpcPassowrd());
        executableAndArgs.add("--bitcoind.rpchost=127.0.0.1:" + Bitcoind.RPC_PORT);
        executableAndArgs.add("--bitcoind.zmqpubrawblock=tcp://127.0.0.1:28332");
        executableAndArgs.add("--bitcoind.zmqpubrawtx=tcp://127.0.0.1:28333");

        setExecutableAndArgs(executableAndArgs.toArray(new String[0]));
        startProcess(context, TAG);
        LightningdState.getInstance().startMonitor();
    }

    public synchronized void stopService(Context context) {
        android.util.Log.i("XXX", "LND : stopService");
        LightningdState.getInstance().stopMonitor();
        stopProcess();
    }
}

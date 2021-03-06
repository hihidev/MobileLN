package com.mobileln.lightningd.clightning;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.util.ArrayList;

import com.mobileln.bitcoind.Bitcoind;
import com.mobileln.lightningd.LightningdState;
import com.mobileln.utils.FileUtils;
import com.mobileln.utils.ProcessHelper;

public class CLightningd extends ProcessHelper {
    private static final String TAG = "Lightningd";
    private static final int BUFFER_SIZE = 100;
    private static final int START_SERVICE_MAX_RETRY = 10;
    private static final int START_SERVICE_RETRY_INTERVAL_MS = 300;
    private static final CLightningd INSTANCE = new CLightningd();

    private CLightningd() {
        super(BUFFER_SIZE, true);
    }

    public static CLightningd getInstance() {
        return INSTANCE;
    }

    @WorkerThread
    public synchronized void startService(Context context) throws IOException {
        for (int i = 0; i < START_SERVICE_MAX_RETRY && isRunning(); i++) {
            SystemClock.sleep(START_SERVICE_RETRY_INTERVAL_MS);
        }
        final String executable = FileUtils.getLightningdExecutable(context);
        final String lightningDataDir = FileUtils.getLightningdDataFolderPath(context);
        final String lightningConfigFile = FileUtils.getLightningdConfigFilePath(context);
        final String lightningRPCFile = FileUtils.getLightningdRPCPath(context);
        final String bitcoinCliExecutable = FileUtils.getBitcoinCliExecutable(context);
        final String bitcoindDataFolderPath = FileUtils.getBitcoindDataFolderPath(context);

        ArrayList<String> executableAndArgs = new ArrayList<>();
        executableAndArgs.add(executable);
        executableAndArgs.add("--lightning-dir=" + lightningDataDir);
        executableAndArgs.add("--conf=" + lightningConfigFile);
        executableAndArgs.add("--rpc-file=" + lightningRPCFile);
        executableAndArgs.add("--bitcoin-cli=" + bitcoinCliExecutable);
        executableAndArgs.add("--bitcoin-datadir=" + bitcoindDataFolderPath);
        executableAndArgs.add("--bitcoin-rpcuser=" + Bitcoind.getInstance().getRpcUserName());
        executableAndArgs.add("--bitcoin-rpcpassword=" + Bitcoind.getInstance().getRpcPassowrd());
        executableAndArgs.add("--bitcoin-rpcport=" + Bitcoind.RPC_PORT);

        setExecutableAndArgs(executableAndArgs.toArray(new String[0]));
        startProcess(context, TAG);
        LightningdState.getInstance().startMonitor();
    }

    public synchronized void stopService(Context context) {
        LightningdState.getInstance().stopMonitor();
        stopProcess();
    }
}

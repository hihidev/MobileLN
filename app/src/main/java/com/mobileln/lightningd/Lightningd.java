package com.mobileln.lightningd;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.WorkerThread;

import java.io.IOException;

import com.mobileln.bitcoind.Bitcoind;
import com.mobileln.utils.FileUtils;
import com.mobileln.utils.ProcessHelper;

public class Lightningd extends ProcessHelper {
    private static final String TAG = "Lightningd";
    private static final int BUFFER_SIZE = 100;
    private static final int START_SERVICE_MAX_RETRY = 10;
    private static final int START_SERVICE_RETRY_INTERVAL_MS = 300;
    private static final Lightningd INSTANCE = new Lightningd();

    private Lightningd() {
        super(BUFFER_SIZE, true);
    }

    public static Lightningd getInstance() {
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
        setExecutableAndArgs(executable,
                "--lightning-dir=" + lightningDataDir,
                "--conf=" + lightningConfigFile,
                "--rpc-file=" + lightningRPCFile,
                "--bitcoin-cli=" + bitcoinCliExecutable,
                "--bitcoin-datadir=" + bitcoindDataFolderPath,
                "--bitcoin-rpcport=" + Bitcoind.RPC_PORT,
                "--bitcoin-rpcuser=" + Bitcoind.getInstance().getRpcUserName(),
                "--bitcoin-rpcpassword=" + Bitcoind.getInstance().getRpcPassowrd());
        startProcess(context, TAG);
        LightningdState.getInstance().startMonitor();
    }

    public synchronized void stopService(Context context) {
        LightningdState.getInstance().stopMonitor();
        stopProcess();
    }
}

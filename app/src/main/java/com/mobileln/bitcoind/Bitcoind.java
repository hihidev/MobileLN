package com.mobileln.bitcoind;

import android.content.Context;
import android.os.SystemClock;

import java.io.IOException;
import java.security.SecureRandom;

import com.mobileln.BuildConfig;
import com.mobileln.utils.FileUtils;
import com.mobileln.utils.ProcessHelper;

public class Bitcoind extends ProcessHelper {
    private static final String TAG = "Bitcoind";
    private static final int BUFFER_SIZE = 100;
    private static final int START_SERVICE_MAX_RETRY = 10;
    private static final int START_SERVICE_RETRY_INTERVAL_MS = 300;
    private static boolean DEBUG = false;
    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Bitcoind INSTANCE = new Bitcoind();
    public static final int RPC_PORT = 8332;

    private String mRpcUserName = null;
    private String mRpcPassowrd = null;

    private Bitcoind() {
        super(BUFFER_SIZE, true);
    }

    public static Bitcoind getInstance() {
        return INSTANCE;
    }

    private static String randomString(int count) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; ++i) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public synchronized void startService(Context context) throws IOException {
        for (int i = 0; i < START_SERVICE_MAX_RETRY && isRunning(); i++) {
            SystemClock.sleep(START_SERVICE_RETRY_INTERVAL_MS);
        }
        if (isRunning()) {
            throw new IOException("Still running...return");
        }
        if (DEBUG || BuildConfig.DEBUG) {
            mRpcUserName = "debugdebugdebug";
            mRpcPassowrd = "debugdebugdebug";
        } else {
            mRpcUserName = randomString(20);
            mRpcPassowrd = randomString(20);
        }
        final String executable = FileUtils.getBitcoindExecutable(context);
        final String dataDir = FileUtils.getBitcoindDataFolderPath(context);
        final String configFile = FileUtils.getBitcoindConfigFilePath(context);
        setExecutableAndArgs(executable,
                "-conf=" + configFile,
                "-datadir=" + dataDir,
                "-rpcuser=" + mRpcUserName,
                "-rpcpassword=" + mRpcPassowrd,
                "-rpcport=" + RPC_PORT,
                "-zmqpubrawblock=tcp://127.0.0.1:28332",
                "-zmqpubrawtx=tcp://127.0.0.1:28333"
        );
        startProcess(context, TAG);
        BitcoindState.getInstance().startMonitor();
    }

    public synchronized void stopService(Context context) {
        BitcoindState.getInstance().stopMonitor();
        stopProcess();
        mRpcUserName = null;
        mRpcPassowrd = null;
    }

    public String getRpcUserName() {
        return mRpcUserName;
    }

    public String getRpcPassowrd() {
        return mRpcPassowrd;
    }
}
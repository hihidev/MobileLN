package com.mobileln.lightningd.lnd;

import android.content.Context;

import com.mobileln.utils.FileUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LndConfig {

    public static final Map<String, String> DEFAULT_LIGHTNINGD_TESTNET_CONFIG;
    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("bitcoin.active", "1");
        map.put("bitcoin.node", "bitcoind");
        map.put("noseedbackup", "1");

        map.put("debuglevel", "debug");
        map.put("bitcoin.testnet", "1");
        DEFAULT_LIGHTNINGD_TESTNET_CONFIG = Collections.unmodifiableMap(map);
    }

    public static void saveConfig(Context context, Map<String, String> map) throws IOException {
        FileUtils.storeConfig(FileUtils.getLndConfigFilePath(context), map);
    }

    public static Map<String, String> readConfig(Context context) throws IOException {
        return FileUtils.readConfig(FileUtils.getLndConfigFilePath(context));
    }

    // TODO: Handle production case
    public static Map<String, String> readDefaultConfig() {
        return DEFAULT_LIGHTNINGD_TESTNET_CONFIG;
    }
}

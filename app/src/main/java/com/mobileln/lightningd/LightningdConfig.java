package com.mobileln.lightningd;

import android.content.Context;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mobileln.utils.FileUtils;

public class LightningdConfig {

    public static final Map<String, String> DEFAULT_LIGHTNINGD_TESTNET_CONFIG;
    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("cltv-delta", "144");
        map.put("fee-base", "950");
        map.put("fee-per-satoshi", "1000");
        map.put("autocleaninvoice-cycle", "1");
        map.put("autocleaninvoice-expired-by", "1");

        map.put("log-level", "debug");
        map.put("network", "testnet");
        DEFAULT_LIGHTNINGD_TESTNET_CONFIG = Collections.unmodifiableMap(map);
    }

    public static void saveConfig(Context context, Map<String, String> map) throws IOException {
        FileUtils.storeConfig(FileUtils.getLightningdConfigFilePath(context), map);
    }

    public static Map<String, String> readConfig(Context context) throws IOException {
        return FileUtils.readConfig(FileUtils.getLightningdConfigFilePath(context));
    }

    // TODO: Handle production case
    public static Map<String, String> readDefaultConfig() {
        return DEFAULT_LIGHTNINGD_TESTNET_CONFIG;
    }
}

package com.mobileln.bitcoind;

import android.content.Context;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mobileln.utils.FileUtils;

// TODO: support production server
public class BitcoindConfig {

    private static final String TESTNET_ASSUME_VALID_HASH =
            "00000000000000d3c36726790caaef294520aa3e5e8dc542ca468402bc216355";
    private static final Map<String, String> DEFAULT_BITCOIND_TESTNET_CONFIG;

    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("maxconnections", "40");
        map.put("maxuploadtarget", "10");
        map.put("dbcache", "100");
        map.put("maxorphantx", "10");
        map.put("maxmempool", "50");
        map.put("upnp", "1");
        map.put("txindex", "0");
        map.put("server", "1");
        map.put("walletrbf", "1");
        map.put("prune", "550");
        map.put("testnet", "1");
        map.put("assumevalid", TESTNET_ASSUME_VALID_HASH);
        DEFAULT_BITCOIND_TESTNET_CONFIG = Collections.unmodifiableMap(map);
    }

    public synchronized static void saveConfig(Context context, Map<String, String> map)
            throws IOException {
        FileUtils.storeConfig(FileUtils.getBitcoindConfigFilePath(context), map);
    }

    public synchronized static Map<String, String> readConfig(Context context) throws IOException {
        return FileUtils.readConfig(FileUtils.getBitcoindConfigFilePath(context));
    }

    public static Map<String, String> readDefaultConfig() {
        return new HashMap<>(DEFAULT_BITCOIND_TESTNET_CONFIG);
    }
}

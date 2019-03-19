package com.mobileln.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileUtils {

    private static final String CONFIG_FOLDER_NAME = "config";

    private static final String BITCOIND_CONFIG_FILE_NAME = "bitcoind.conf";
    private static final String LIGHTNINGD_CONFIG_FILE_NAME = "lightningd.conf";

    private static final String BITCOIND_DATA_FOLDER_NAME = "bitcoind_data";
    private static final String LIGHTNINGD_DATA_FOLDER_NAME = "lightningd_data";

    private static final String ASSETS_FOLDER_NAME = "assets";
    public static final String EXECUTABLES_FOLDER_NAME = "executables";
    private static final String BITCOIND_EXECUTABLE =
            ASSETS_FOLDER_NAME + "/" + EXECUTABLES_FOLDER_NAME + "/bitcoind/bitcoind";
    private static final String BITCOIN_CLI_EXECUTABLE =
            ASSETS_FOLDER_NAME + "/" + EXECUTABLES_FOLDER_NAME + "/bitcoind/bitcoin-cli";
    private static final String LIGHTNINGD_EXECUTABLE =
            ASSETS_FOLDER_NAME + "/" + EXECUTABLES_FOLDER_NAME
                    + "/lightningd/usr/local/bin/lightningd";
    private static final String LIGHTNING_CLI_EXECUTABLE =
            ASSETS_FOLDER_NAME + "/" + EXECUTABLES_FOLDER_NAME
                    + "/lightningd/usr/local/bin/lightning-cli";
    private static final String LIGHTNING_RPC_FILE = "lightningrpc";
    public static final String TMP_FASTSYNC_FILE = "tmpfastsyncfile";

    private static String getFolderPath(File parentDir, String folderName) throws IOException {
        File folder = new File(parentDir, folderName);
        folder.mkdirs();
        return folder.getCanonicalPath();
    }

    public static String getBitcoindConfigFilePath(Context context) throws IOException {
        File folder = new File(context.getFilesDir(), CONFIG_FOLDER_NAME);
        folder.mkdirs();
        return new File(folder, BITCOIND_CONFIG_FILE_NAME).getCanonicalPath();
    }

    public static String getLightningdConfigFilePath(Context context) throws IOException {
        File folder = new File(context.getFilesDir(), CONFIG_FOLDER_NAME);
        folder.mkdirs();
        return new File(folder, LIGHTNINGD_CONFIG_FILE_NAME).getCanonicalPath();
    }

    public static String getBitcoindDataFolderPath(Context context) throws IOException {
        return getFolderPath(context.getFilesDir(), BITCOIND_DATA_FOLDER_NAME);
    }

    public static String getLightningdDataFolderPath(Context context) throws IOException {
        return getFolderPath(context.getFilesDir(), LIGHTNINGD_DATA_FOLDER_NAME);
    }

    public static String getBitcoindExecutable(Context context) throws IOException {
        return new File(context.getFilesDir(), BITCOIND_EXECUTABLE).getCanonicalPath();
    }

    public static String getBitcoinCliExecutable(Context context) throws IOException {
        return new File(context.getFilesDir(), BITCOIN_CLI_EXECUTABLE).getCanonicalPath();
    }

    public static String getLightningdExecutable(Context context) throws IOException {
        return new File(context.getFilesDir(), LIGHTNINGD_EXECUTABLE).getCanonicalPath();
    }

    public static String getLightningCliExecutable(Context context) throws IOException {
        return new File(context.getFilesDir(), LIGHTNING_CLI_EXECUTABLE).getCanonicalPath();
    }

    public static String getAssetsFolderPath(Context context) throws IOException {
        return getFolderPath(context.getFilesDir(), ASSETS_FOLDER_NAME);
    }

    public static String getLightningdRPCPath(Context context) throws IOException {
        return new File(context.getFilesDir(), LIGHTNING_RPC_FILE).getCanonicalPath();
    }

    public static void storeConfig(String filePath, Map<String, String> map)
            throws IOException {
        Properties properties = new Properties();
        properties.putAll(map);
        properties.store(new FileOutputStream(filePath), "config");
    }

    public static Map<String, String> readConfig(String filePath) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(filePath));
        HashMap<String, String> map = new HashMap<>();
        for (final String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
        return map;
    }
}

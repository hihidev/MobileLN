package com.mobileln.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mobileln.R;

public class ExtractResourceUtilsDeprecated {

    private static final String TAG = "ExtractResourceUtils";
    private static final String LAST_EXECUTABLE_VERSION = "last_executable_version";

    public static boolean requiresUpdate(Context context) {
        int currentVersion = context.getResources().getInteger(R.integer.current_executable_version);
        SharedPreferences sp = context.getSharedPreferences(LAST_EXECUTABLE_VERSION, Context.MODE_PRIVATE);
        int lastVersion = sp.getInt(LAST_EXECUTABLE_VERSION, 0);
        Log.i(TAG, "Last executable version: " + lastVersion);
        Log.i(TAG, "Current executable version: " + currentVersion);
        return currentVersion > lastVersion;
    }

    private static void updateLastExecutableVersion(Context context) {
        int currentVersion = context.getResources().getInteger(R.integer.current_executable_version);
        SharedPreferences sp = context.getSharedPreferences(LAST_EXECUTABLE_VERSION, Context.MODE_PRIVATE);
        sp.edit().putInt(LAST_EXECUTABLE_VERSION, currentVersion).apply();
        Log.i(TAG, "Updated executable to version: " + currentVersion);
    }

    private static void removeAllExecutables(Context context) throws IOException {
        String executableFolder = FileUtils.getAssetsFolderPath(context);
        deleteDirectory(new File(executableFolder));
    }

    private static boolean deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

    private static void extractExecutable(Context context, String assetPath) throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] files = assetManager.list(assetPath);
        String executableFolder = FileUtils.getAssetsFolderPath(context);
        Log.i(TAG, "Extracting " + assetPath);
        File outFile = new File(executableFolder, assetPath);
        if (files.length == 0) {
            try (
                    InputStream in = assetManager.open(assetPath);
                    OutputStream out = new FileOutputStream(outFile)
            ){
                copyFile(in, out);
                outFile.setExecutable(true);
            }
        } else {
            outFile.mkdirs();
            for (String file : files) {
                extractExecutable(context, assetPath + "/" + file);
            }
        }

    }

    private static void extractAllExecutables(Context context) throws IOException {
        Log.i(TAG, "Start extracting executables");
        // extractExecutable(context, FileUtils.EXECUTABLES_FOLDER_NAME);
        Log.i(TAG, "Finished extracting executables");
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public synchronized static void extractExecutablesIfNecessary(Context context, boolean force) throws IOException {
        if (!force && !requiresUpdate(context)) {
            return;
        }
        Log.i(TAG, "Requires update executables");
        removeAllExecutables(context);
        extractAllExecutables(context);
        updateLastExecutableVersion(context);
    }
}

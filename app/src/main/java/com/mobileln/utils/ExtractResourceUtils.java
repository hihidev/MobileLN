package com.mobileln.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mobileln.R;

public class ExtractResourceUtils {

    private static final String TAG = "ExtractResourceUtils";

    private static void removeAllExecutables(Context context) throws IOException {
        String executableFolder = FileUtils.getNativeExecutablesFolder(context);
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

    private static void extractExecutable(Context context, String assetPathPrefix, String filePath) throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] files = assetManager.list(assetPathPrefix + filePath);
        String executableFolder = FileUtils.getNativeExecutablesFolder(context);
        Log.i(TAG, "Extracting " + assetPathPrefix + filePath);
        File outFile = new File(executableFolder, filePath);
        if (files.length == 0) {
            try (
                    InputStream in = assetManager.open(assetPathPrefix + filePath);
                    OutputStream out = new FileOutputStream(outFile)
            ) {
                copyFile(in, out);
                outFile.setExecutable(true);
            }
        } else {
            outFile.mkdirs();
            for (String file : files) {
                extractExecutable(context, assetPathPrefix, filePath + "/" + file);
            }
        }

    }

    private static String getBestABI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (String abi : Build.SUPPORTED_ABIS) {
                if ("arm64-v8a".equals(abi)) {
                    return "arm64-v8a";
                }
            }
        }
        return "armeabi-v7a";
    }

    private static void extractAllExecutables(Context context) throws IOException {
        Log.i(TAG, "Start extracting executables");
        extractExecutable(context, "executables/" + getBestABI() + "", "");
        Log.i(TAG, "Finished extracting executables");
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public synchronized static void extractExecutables(Context context)
            throws IOException {
        removeAllExecutables(context);
        extractAllExecutables(context);
    }
}

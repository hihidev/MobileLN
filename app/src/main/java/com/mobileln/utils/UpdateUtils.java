package com.mobileln.utils;

import android.content.Context;
import android.support.annotation.WorkerThread;

import com.mobileln.bitcoind.BitcoindConfig;
import com.mobileln.lightningd.LightningdConfig;

import java.io.IOException;

public class UpdateUtils {
    @WorkerThread
    public static void startUpdate(Context context) {
        try {
            ExtractResourceUtils.extractExecutables(context);
            if (SettingsSharedPrefs.getInstance(context).useDefaultConfigs()) {
                BitcoindConfig.saveConfig(context, BitcoindConfig.readDefaultConfig());
                LightningdConfig.saveDefaultConfig(context);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        SettingsSharedPrefs.getInstance(context).setAppUpdated();
    }
}

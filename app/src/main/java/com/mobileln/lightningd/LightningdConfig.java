package com.mobileln.lightningd;

import android.content.Context;

import com.mobileln.MyApplication;
import com.mobileln.lightningd.clightning.CLightningdConfig;
import com.mobileln.lightningd.lnd.LndConfig;
import com.mobileln.utils.SettingsSharedPrefs;

import java.io.IOException;

public class LightningdConfig {

    private static boolean isLnd() {
        boolean useLnd = new SettingsSharedPrefs(MyApplication.getContext()).isBackendLnd();
        return useLnd;
    }

    public static boolean isValidConfig(Context context) throws IOException {
        if (isLnd()) {
            return LndConfig.readConfig(context).size() > 0;
        } else {
            return CLightningdConfig.readConfig(context).size() > 0;
        }
    }

    public static void saveDefaultConfig(Context context) throws IOException {
        if (isLnd()) {
            LndConfig.saveConfig(context,
                    LndConfig.readDefaultConfig());
        } else {
            CLightningdConfig.saveConfig(context,
                    CLightningdConfig.readDefaultConfig());
        }
    }
}

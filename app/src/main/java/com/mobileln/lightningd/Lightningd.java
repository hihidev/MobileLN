package com.mobileln.lightningd;

import android.content.Context;

import com.mobileln.MyApplication;
import com.mobileln.lightningd.clightning.CLightningd;
import com.mobileln.lightningd.lnd.Lnd;
import com.mobileln.utils.SettingsSharedPrefs;

import java.io.IOException;

public class Lightningd {

    private static boolean isLnd() {
        boolean useLnd = SettingsSharedPrefs.getInstance(MyApplication.getContext()).isBackendLnd();
        return useLnd;
    }

    public static void startService(Context context) throws IOException {
        if (isLnd()) {
            Lnd.getInstance().startService(context);
        }  else {
            CLightningd.getInstance().startService(context);
        }
    }

    public static void stopService(Context context) {
        if (isLnd()) {
            Lnd.getInstance().stopService(context);
        }  else {
            CLightningd.getInstance().stopService(context);
        }
    }

    public static boolean isRunning() {
        if (isLnd()) {
            return Lnd.getInstance().isRunning();
        }  else {
            return CLightningd.getInstance().isRunning();
        }
    }

    public static boolean fullyStopped() {
        if (isLnd()) {
            return Lnd.getInstance().fullyStopped();
        }  else {
            return CLightningd.getInstance().fullyStopped();
        }
    }
}

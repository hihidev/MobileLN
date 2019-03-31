package com.mobileln.lightningd;

import android.content.Context;

import com.mobileln.MyApplication;
import com.mobileln.lightningd.clightning.CLightningd;
import com.mobileln.utils.SettingsSharedPrefs;

import java.io.IOException;

public class Lightningd {

    private static boolean isLnd() {
        boolean useLnd = new SettingsSharedPrefs(MyApplication.getContext()).isBackendLnd();
        return useLnd;
    }

    public static void startService(Context context) throws IOException {
        if (isLnd()) {

        }  else {
            CLightningd.getInstance().startService(context);
        }
    }

    public static void stopService(Context context) {
        if (isLnd()) {

        }  else {
            CLightningd.getInstance().stopService(context);
        }
    }
}

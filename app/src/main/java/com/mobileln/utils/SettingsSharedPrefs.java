package com.mobileln.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsSharedPrefs {
    private static final String SHARED_PREFS_NAME = "settings";
    private static final String LIGHTNINGD_BACKEND = "lightningd_backend";
    private static final String LIGHTNINGD_BACKEND_CLIGHTNING = "clightning";
    private static final String LIGHTNINGD_BACKEND_LND = "lnd";

    private SharedPreferences mSharedPreferences;

    public SettingsSharedPrefs(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isBackendLnd() {
        return mSharedPreferences.getString(LIGHTNINGD_BACKEND, LIGHTNINGD_BACKEND_CLIGHTNING).equals(
                LIGHTNINGD_BACKEND_LND);
    }

    public void setBackendIsLnd(boolean isLnd) {
        if (isLnd) {
            mSharedPreferences.edit().putString(LIGHTNINGD_BACKEND, LIGHTNINGD_BACKEND_LND).apply();
        } else {
            mSharedPreferences.edit().putString(LIGHTNINGD_BACKEND,
                    LIGHTNINGD_BACKEND_CLIGHTNING).apply();
        }
    }
}

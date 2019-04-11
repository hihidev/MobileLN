package com.mobileln.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.mobileln.BuildConfig;
import com.mobileln.MyApplication;

public class SettingsSharedPrefs {
    private static final String SHARED_PREFS_NAME = "settings";
    private static final String LIGHTNINGD_BACKEND = "lightningd_backend";
    private static final String LIGHTNINGD_BACKEND_CLIGHTNING = "clightning";
    private static final String LIGHTNINGD_BACKEND_LND = "lnd";
    private static final String USE_DEFAULT_CONFIGS = "use_default_configs";
    private static final String LAST_APP_VERSION = "last_app_version";

    private static SharedPreferences mSharedPreferences;
    private static SettingsSharedPrefs sInstance;

    private SettingsSharedPrefs(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SettingsSharedPrefs getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SettingsSharedPrefs(context);
        }
        return sInstance;
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

    public boolean useDefaultConfigs() {
        return mSharedPreferences.getBoolean(USE_DEFAULT_CONFIGS, true);
    }

    public void setUseDefaultConfigs(boolean useDefault) {
        mSharedPreferences.edit().putBoolean(USE_DEFAULT_CONFIGS, useDefault).apply();
    }

    public boolean isAppUpdated() {
        long lastAppVersion = mSharedPreferences.getLong(LAST_APP_VERSION, -1);
        return BuildConfig.VERSION_CODE == lastAppVersion;
    }

    public void setAppUpdated() {
        mSharedPreferences.edit().putLong(LAST_APP_VERSION, BuildConfig.VERSION_CODE).apply();
    }
}

package com.mobileln.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class WalletStateSharedPrefs {
    private static final String SHARED_PREFS_NAME = "wallet_state";
    private static final String WATCH_ONLY_MAX_INDEX = "watch_only_max_index";

    private SharedPreferences mSharedPreferences;

    public WalletStateSharedPrefs(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getWatchOnlyMaxIndex() {
        return mSharedPreferences.getInt(WATCH_ONLY_MAX_INDEX, 0);
    }

    public void saveWatchOnlyMaxIndex(int index) {
        mSharedPreferences.edit().putInt(WATCH_ONLY_MAX_INDEX, index).apply();
    }
}

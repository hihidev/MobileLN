package com.mobileln.utils;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class UIUtils {
    private static final String TAG = "UIUtils";

    public static void showErrorToast(final Activity activity, final String message) {
        showToast(activity, "Error: " + message);
    }
    public static void showToast(final Activity activity, final String message) {
        if (activity == null) {
            Log.e(TAG, message);
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}

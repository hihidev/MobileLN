package com.mobileln.utils;

import android.util.Pair;

public class BtcSatUtils {
    public static String sat2String(long sat) {
        if (sat < 100000) {
            return sat + " sat";
        } else if (sat < 1000000) {
            return sat/1000 + "k sat";
        } else {
            return String.format("\u20BF%.4f", (sat / (double)100000000));
        }
    }

    public static Pair<String, String> sat2StringPair(long sat) {
        if (sat < 100000) {
            return Pair.create(String.valueOf(sat), "SAT");
        } else if (sat < 1000000) {
            return Pair.create(String.valueOf(sat/1000) + "k", "SAT");
        } else {
            return Pair.create(String.format("%.4f", (sat / (double)100000000)), "BTC");
        }
    }
}

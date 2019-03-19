package com.mobileln.utils;

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
}

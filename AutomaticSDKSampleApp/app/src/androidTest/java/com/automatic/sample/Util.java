package com.automatic.sample;

import android.text.format.DateUtils;

/**
 * Created by stevenzhou on 12/8/15.
 */
public class Util {
    public static void threadWait(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void threadWait() {
        threadWait(100);
    }

    public static long oneWeekAgo() {
        return System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 7;
    }

    public static long twoWeekAgo() {
        return System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 14;
    }
}

package com.mobileln.utils;

public class CircularArray {

    private final int mMaxSize;
    private final String[] mArray;
    private int mNextIndex = 0;

    public CircularArray(int maxSize) {
        mMaxSize = maxSize;
        mArray = new String[maxSize];
    }

    public String get(int i) {
        return mArray[(i + mNextIndex) % mMaxSize];
    }

    public String get() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mMaxSize; i++) {
            String str = mArray[(i + mNextIndex) % mMaxSize];
            if (str != null) {
                builder.append(mArray[(i + mNextIndex) % mMaxSize]);
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    public void add(String str) {
        mArray[mNextIndex] = str;
        mNextIndex = (mNextIndex + 1) % mMaxSize;
    }
}

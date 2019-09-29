package com.haiyunshan.signal;

import android.app.Application;

public class SignalApp extends Application {

    private static SignalApp sInstance;

    public static final SignalApp instance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
    }

}

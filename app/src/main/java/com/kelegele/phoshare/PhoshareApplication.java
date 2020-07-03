package com.kelegele.phoshare;

import android.app.Application;

import timber.log.Timber;

public class PhoshareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}

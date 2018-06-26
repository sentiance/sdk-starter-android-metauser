package com.sentiance.sdkstarter;

import android.app.Application;

public class MyApplication extends Application {

    @Override
    public void onCreate () {
        super.onCreate();
        new SentianceWrapper(this).initializeSentianceSdk();
    }
}

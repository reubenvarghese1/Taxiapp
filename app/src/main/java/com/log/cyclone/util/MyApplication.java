package com.log.cyclone.util;

import android.app.Application;
import android.content.Context;

/**
 * Created by saminda on 12/12/16.
 */

public class MyApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}

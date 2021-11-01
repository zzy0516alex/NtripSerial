package com.gnss.ntripserial.Utils;

import android.app.Application;

import java.io.File;

/**
 * in AndroidManifest.xml
 * android:name=".Global.MyApplication"
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }

}

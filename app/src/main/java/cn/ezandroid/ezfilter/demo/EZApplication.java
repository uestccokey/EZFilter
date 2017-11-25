package cn.ezandroid.ezfilter.demo;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.squareup.leakcanary.LeakCanary;

/**
 * EZApplication
 *
 * @author like
 * @date 2017-09-18
 */
public class EZApplication extends Application {

    public static EZApplication gContext = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        gContext = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}

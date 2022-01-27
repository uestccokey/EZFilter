package cn.ezandroid.ezfilter;

import android.app.Application;
import android.content.Context;

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
        gContext = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

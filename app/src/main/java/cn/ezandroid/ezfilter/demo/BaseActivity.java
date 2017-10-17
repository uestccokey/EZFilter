package cn.ezandroid.ezfilter.demo;

import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Activity基类
 *
 * @author like
 * @date 2017-09-15
 */
public class BaseActivity extends AppCompatActivity {

    public <T extends View> T $(@IdRes int resId) {
        return (T) findViewById(resId);
    }

    public <T extends View> T $(View layoutView, @IdRes int resId) {
        return (T) layoutView.findViewById(resId);
    }
}

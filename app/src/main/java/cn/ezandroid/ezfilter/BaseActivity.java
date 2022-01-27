package cn.ezandroid.ezfilter;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

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

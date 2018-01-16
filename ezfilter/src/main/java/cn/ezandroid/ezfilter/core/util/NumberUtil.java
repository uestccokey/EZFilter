package cn.ezandroid.ezfilter.core.util;

import android.text.TextUtils;

/**
 * 数字工具类
 *
 * @author like
 * @date 2018-01-16
 */
public class NumberUtil {

    public static int parseInt(String string) {
        return parseInt(string, 0);
    }

    public static int parseInt(String string, int def) {
        if (TextUtils.isEmpty(string)) return def;
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return def;
    }
}

package cn.ezandroid.ezfilter.core.util;

import android.text.TextUtils;

/**
 * 数字工具类
 *
 * @author like
 * @date 2018-01-16
 */
public class NumberUtil {

    public static int parseInt(String val) {
        return parseInt(val, 0);
    }

    public static int parseInt(String val, int def) {
        if (TextUtils.isEmpty(val)) return def;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return def;
    }

    public static long parseLong(String val) {
        return parseLong(val, 0);
    }

    public static long parseLong(String val, long def) {
        if (TextUtils.isEmpty(val)) return def;
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }
}

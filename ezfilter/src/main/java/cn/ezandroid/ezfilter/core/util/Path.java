package cn.ezandroid.ezfilter.core.util;

import java.util.Locale;

/**
 * 路径前缀枚举
 *
 * @author like
 * @date 2018-01-15
 */
public enum Path {

    FILE("file://"), // file:// + /sdcard/demo.mp4
    ASSETS("file:///android_asset/"), // file:///android_asset/ + demo.mp4
    DRAWABLE("drawable://"), // drawable:// + 3243342
    UNKNOWN("");

    private String mScheme;

    private Path(String scheme) {
        this.mScheme = scheme;
    }

    public static Path ofUri(String uri) {
        if (uri != null) {
            Path[] var1 = values();
            for (Path s : var1) {
                if (s.belongsTo(uri)) {
                    return s;
                }
            }
        }

        return UNKNOWN;
    }

    public boolean belongsTo(String uri) {
        return uri.toLowerCase(Locale.US).startsWith(this.mScheme);
    }

    public String wrap(String path) {
        return this.mScheme + path;
    }

    public String crop(String uri) {
        if (!this.belongsTo(uri)) {
            throw new IllegalArgumentException(String.format("URI [%1$s] doesn't have expected path [%2$s]", new Object[]{uri, this.mScheme}));
        } else {
            return uri.substring(this.mScheme.length());
        }
    }
}

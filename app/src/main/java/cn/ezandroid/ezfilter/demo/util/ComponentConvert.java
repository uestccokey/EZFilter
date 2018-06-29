package cn.ezandroid.ezfilter.demo.util;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.ezandroid.ezfilter.core.util.NumberUtil;
import cn.ezandroid.ezfilter.core.util.Path;
import cn.ezandroid.ezfilter.extra.sticker.model.Component;

/**
 * 贴纸组件转换器
 *
 * @author like
 * @date 2018-01-15
 */
public class ComponentConvert {

    /**
     * 将读取到的原始贴纸组件模型转换为更易使用的格式
     * <p>
     * 1，使用传入的dir，转换src为绝对路径，方便后面直接进行读取
     * 2，获取并设置素材文件夹内有效文件的数量length
     * 3，获取并设置素材文件路径列表resources
     *
     * @param context
     * @param component
     * @param dir
     * @throws IOException
     */
    public static void convert(Context context, Component component, String dir) {
        // 这里修正资源文件路径为"绝对"路径方便以后读取
        component.src = dir + component.src;
        String names[] = null;
        if (Path.ASSETS.belongsTo(component.src)) {
            try {
                names = context.getAssets().list(Path.ASSETS.crop(component.src));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Path.FILE.belongsTo(component.src)) {
            names = new File(Path.FILE.crop(component.src)).list();
        } else {
            names = new File(component.src).list();
        }

        // 过滤.DS_Store和Thumbs.db文件
        List<String> paths = new ArrayList<>();
        if (names != null) {
            for (String name : names) {
                if (name.toLowerCase().contains(".DS_Store".toLowerCase())
                        || name.toLowerCase().contains("Thumbs.db".toLowerCase())) {
                    continue;
                }
                paths.add(component.src + "/" + name);
            }
        }
        component.length = paths.size();

        // 根据图片文件名进行排序
        Collections.sort(paths, (o1, o2) -> {
            // 最后一个下划线_之后及小数点.之前的数字
            String[] sp1 = o1.split("_");
            String[] sp2 = o2.split("_");
            String num1 = sp1[sp1.length - 1];
            if (num1.contains(".")) {
                num1 = num1.substring(0, num1.indexOf("."));
            }
            if (!TextUtils.isDigitsOnly(num1)) {
                num1 = num1.substring(num1.length() - 2);
            }
            String num2 = sp2[sp2.length - 1];
            if (num2.contains(".")) {
                num2 = num2.substring(0, num2.indexOf("."));
            }
            if (!TextUtils.isDigitsOnly(num2)) {
                num2 = num2.substring(num2.length() - 2);
            }
            return NumberUtil.parseInt(num1) - NumberUtil.parseInt(num2);
        });
        component.resources = paths;
    }
}

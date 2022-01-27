package cn.ezandroid.ezfilter.render;

import android.content.Context;

import java.util.ArrayList;

import cn.ezandroid.ezfilter.util.ComponentConvert;
import cn.ezandroid.lib.ezfilter.extra.sticker.StickerRender;
import cn.ezandroid.lib.ezfilter.extra.sticker.model.AnchorPoint;
import cn.ezandroid.lib.ezfilter.extra.sticker.model.Component;
import cn.ezandroid.lib.ezfilter.extra.sticker.model.ScreenAnchor;
import cn.ezandroid.lib.ezfilter.extra.sticker.model.Sticker;
import cn.ezandroid.lib.ezfilter.extra.sticker.model.TextureAnchor;

/**
 * SnowStickerRender
 *
 * @author like
 * @date 2018-05-25
 */
public class SnowStickerRender extends StickerRender {

    public SnowStickerRender(Context context) {
        super(context);

        Sticker sticker = new Sticker();
        sticker.components = new ArrayList<>();
        Component component = new Component();
        component.duration = 3000;
        component.src = "src_1";
        component.width = 281;
        component.height = 500;
        TextureAnchor textureAnchor = new TextureAnchor();
        textureAnchor.leftAnchor = new AnchorPoint(AnchorPoint.LEFT_TOP, 0, 0);
        textureAnchor.rightAnchor = new AnchorPoint(AnchorPoint.RIGHT_TOP, 0, 0);
        textureAnchor.width = component.width;
        textureAnchor.height = component.height;
        component.textureAnchor = textureAnchor;
        sticker.components.add(component);
        ComponentConvert.convert(context, component, "file:///android_asset/snow/");
        setSticker(sticker);

        ScreenAnchor screenAnchor = new ScreenAnchor();
        screenAnchor.leftAnchor = new AnchorPoint(AnchorPoint.LEFT_TOP, 0, 0);
        screenAnchor.rightAnchor = new AnchorPoint(AnchorPoint.RIGHT_TOP, 0, 0);
        setScreenAnchor(screenAnchor);
    }
}

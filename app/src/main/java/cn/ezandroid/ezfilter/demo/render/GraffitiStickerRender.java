package cn.ezandroid.ezfilter.demo.render;

import android.content.Context;
import android.graphics.PointF;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.demo.util.ComponentConvert;
import cn.ezandroid.ezfilter.extra.sticker.StickerRender;
import cn.ezandroid.ezfilter.extra.sticker.model.AnchorPoint;
import cn.ezandroid.ezfilter.extra.sticker.model.Component;
import cn.ezandroid.ezfilter.extra.sticker.model.ScreenAnchor;
import cn.ezandroid.ezfilter.extra.sticker.model.Sticker;
import cn.ezandroid.ezfilter.extra.sticker.model.TextureAnchor;

/**
 * GraffitiStickerRender
 *
 * @author like
 * @date 2018-05-25
 */
public class GraffitiStickerRender extends StickerRender {

    public interface IStickerTimeController {

        float getCurrentTime();
    }

    private float mStartTime;
    private float mEndTime;
    private List<PointF> mPositionHistories = new ArrayList<>(); // 贴纸位置历史

    private IStickerTimeController mTimeController;

    private boolean mIsPause = true;

    public GraffitiStickerRender(Context context, @NotNull IStickerTimeController timeController) {
        super(context);

        Sticker sticker = new Sticker();
        sticker.components = new ArrayList<>();
        Component component = new Component();
        component.duration = 2000;
        component.src = "src_1";
        component.width = 245;
        component.height = 245;
        TextureAnchor textureAnchor = new TextureAnchor();
        textureAnchor.leftAnchor = new AnchorPoint(AnchorPoint.LEFT_BOTTOM, 0, 0);
        textureAnchor.rightAnchor = new AnchorPoint(AnchorPoint.RIGHT_BOTTOM, 0, 0);
        textureAnchor.width = component.width;
        textureAnchor.height = component.height;
        component.textureAnchor = textureAnchor;
        sticker.components.add(component);
        ComponentConvert.convert(context, component, "file:///android_asset/test/");
        setSticker(sticker);

        ScreenAnchor screenAnchor = new ScreenAnchor();
        screenAnchor.leftAnchor = new AnchorPoint(AnchorPoint.LEFT_TOP, 0, 0);
        screenAnchor.rightAnchor = new AnchorPoint(AnchorPoint.LEFT_TOP, 0, 0);
        setScreenAnchor(screenAnchor);

        mTimeController = timeController;
    }

    public List<PointF> getPositionHistories() {
        return mPositionHistories;
    }

    public void setPosition(int x, int y) {
        if (!mSticker.components.isEmpty()) {
            mScreenAnchor.leftAnchor.x = x - mSticker.components.get(0).width / 2;
            mScreenAnchor.leftAnchor.y = y;

            mScreenAnchor.rightAnchor.x = x + mSticker.components.get(0).width / 2; // 涂鸦贴纸只有一个元素
            mScreenAnchor.rightAnchor.y = y;
        }
    }

    public void start() {
        mIsPause = false;
        mStartTime = mTimeController.getCurrentTime();
    }

    public void pause() {
        mIsPause = true;
        mEndTime = mTimeController.getCurrentTime();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (!mIsPause) {
            float x = mScreenAnchor.leftAnchor.x + mSticker.components.get(0).width / 2;
            float y = mScreenAnchor.leftAnchor.y;
            mPositionHistories.add(new PointF(x, y));
        } else {
            float currentTime = mTimeController.getCurrentTime();
            if (currentTime >= mStartTime && mEndTime > mStartTime && !mPositionHistories.isEmpty()) {
                int index = Math.round((mPositionHistories.size() - 1) * (currentTime - mStartTime) / (mEndTime - mStartTime));
                PointF pointF = mPositionHistories.get(index < mPositionHistories.size() ? index : mPositionHistories.size() - 1);
                setPosition(Math.round(pointF.x), Math.round(pointF.y));
            } else {
                setPosition(0, 0);
            }
        }
    }
}

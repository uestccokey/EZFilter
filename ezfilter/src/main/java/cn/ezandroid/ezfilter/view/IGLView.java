package cn.ezandroid.ezfilter.view;

import android.view.Surface;

/**
 * IGLView
 *
 * @author like
 * @date 2017-09-20
 */
public interface IGLView {

    int getWidth();

    int getHeight();

    void setSurface(Surface surface);

    void setRender(IRender render);
}

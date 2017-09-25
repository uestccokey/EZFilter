package cn.ezandroid.ezfilter.core;

/**
 * 终点渲染器
 * <p>
 * 渲染列表最后必须添加一个终点渲染器，不能为FBORender，否则会渲染不出来
 *
 * @author like
 * @date 2017-09-15
 */
public class EndPointRender extends AbstractRender implements OnTextureAcceptableListener {

    @Override
    public void onTextureAcceptable(int texture, FBORender source) {
        mTextureIn = texture;
        setWidth(source.getWidth());
        setHeight(source.getHeight());
        onDrawFrame();
    }
}

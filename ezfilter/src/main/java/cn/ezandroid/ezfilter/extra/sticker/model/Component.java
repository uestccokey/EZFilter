package cn.ezandroid.ezfilter.extra.sticker.model;

import java.io.Serializable;
import java.util.List;

/**
 * 贴纸内部组件
 *
 * @author like
 * @date 2018-01-05
 */
public class Component implements Serializable {

    private static final long serialVersionUID = 1L;

    // 素材文件夹
    public String src;

    // 持续时间
    public int duration;

    // 纹理锚点
    public TextureAnchor textureAnchor;

    // 素材原始宽度 为了进行多分辨率适配和节约内存，加载到内存的图片可能会比素材原始小，而锚点等的信息是根据素材原始尺寸设置的，因此这里进行记录
    public int width;

    // 素材原始高度 为了进行多分辨率适配和节约内存，加载到内存的图片可能会比素材原始小，而锚点等的信息是根据素材原始尺寸设置的，因此这里进行记录
    public int height;

    @Override
    public String toString() {
        return "Component{" +
                "src='" + src + '\'' +
                ", length=" + length +
                '}';
    }

    // 素材文件数 素材文件夹内有效文件的数量
    public int length;

    // 素材文件路径列表
    public List<String> resources;
}

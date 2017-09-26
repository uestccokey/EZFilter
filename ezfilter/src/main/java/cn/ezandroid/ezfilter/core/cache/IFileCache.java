package cn.ezandroid.ezfilter.core.cache;

/**
 * 文件缓存接口
 * <p>
 * 用来缓存已保存的文件，下次再进行保存操作时不进行生成，直接返回
 *
 * @author like
 * @date 2017-09-25
 */
public interface IFileCache {

    String get(String key);

    void put(String key, String output);

    void clear();
}

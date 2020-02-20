package yanry.lib.android.view.animate.reactive.frame;

import java.io.InputStream;

/**
 * 序列帧数据源接口。
 */
public interface AnimateFrameSource {
    /**
     * 判断序列帧是否存在。
     *
     * @return
     */
    boolean exist();

    /**
     * 获取帧数。
     *
     * @return
     */
    int getFrameCount();

    /**
     * 获取指定序号帧的输入流。
     *
     * @param index
     * @return
     */
    InputStream getFrameInputStream(int index);

    /**
     * 实现类应重写toString()方法，用于输出日志。
     *
     * @return
     */
    String toString();
}

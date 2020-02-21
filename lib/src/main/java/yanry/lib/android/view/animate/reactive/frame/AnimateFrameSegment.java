package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.SparseArray;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import yanry.lib.android.entity.MainHandler;
import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.interfaces.OnValueChangeListener;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;

/**
 * 使用序列帧实现的动画片段。具体实现上，使用线程池解码，每个处于活动状态的动画片段占用一个线程，使用队列长度为1的生产者消费者模式进行解码/绘制。
 * 功能上，支持指定播放次数、帧率、反序播放、任意帧开始等特性。
 */
public class AnimateFrameSegment extends BitmapFactory.Options implements AnimateSegment {
    private AnimateFrameSource source;
    private AnimateFrameDecoder decoder;
    private int cacheCapacity;

    private Queue<Frame> cacheQueue;
    private SparseArray<Frame> presetFrames;
    private LinkedList<Bitmap> recycledPool;
    private LinkedList<OnValueChangeListener<Frame>> onFrameUpdateListeners;

    private boolean isActive;
    private int decodeCounter;
    private long refreshTimestamp;
    private Frame currentFrame;

    private int left;
    private int top;
    private int repeatCount;
    private boolean reverse;
    private long refreshInterval;
    private boolean pause;
    private boolean fillEnd;
    private int startIndex;
    private int zOrder;

    public AnimateFrameSegment(AnimateFrameSource source, AnimateFrameDecoder decoder, int cacheCapacity) {
        this.source = source;
        this.decoder = decoder;
        this.cacheCapacity = cacheCapacity;
        this.cacheQueue = new LinkedList<>();
        presetFrames = new SparseArray<>();
        recycledPool = new LinkedList<>();
    }

    /**
     * 设置z坐标值。
     *
     * @param zOrder
     * @return
     */
    public AnimateFrameSegment zOrder(int zOrder) {
        this.zOrder = zOrder;
        return this;
    }

    /**
     * @param count 动画重复播放次数，只有大于0才生效，默认无限重复播放。
     * @return
     */
    public AnimateFrameSegment repeatCount(int count) {
        if (count == Integer.MAX_VALUE) {
            repeatCount = -1;
        } else {
            repeatCount = count;
        }
        return this;
    }

    /**
     * @param interval 序列帧切换的最小时间间隔毫秒，若不指定则跟随AnimateView的刷新率。
     * @return
     */
    public AnimateFrameSegment refreshInterval(long interval) {
        refreshInterval = interval;
        return this;
    }

    /**
     * 预设并缓存指定序号的帧位图，通常用于缓存首帧以提高动画启动速度。
     *
     * @param index
     * @param frameBitmap
     * @return
     */
    public AnimateFrameSegment presetFrame(int index, Bitmap frameBitmap) {
        if (frameBitmap != null) {
            presetFrames.put(index, new Frame(frameBitmap, index, null));
        }
        return this;
    }

    /**
     * 设置播放到最后一帧时若动画未结束再反序播放，默认是从头播放。
     *
     * @return
     */
    public AnimateFrameSegment reverse() {
        reverse = true;
        return this;
    }

    /**
     * 设置动画播放结束时停留在最后一帧，默认播放结束时不再显示动画。
     *
     * @return
     */
    public AnimateFrameSegment fillEnd() {
        fillEnd = true;
        return this;
    }

    /**
     * 设置动画帧左上角的位置（相对于AnimateView）。
     *
     * @param left
     * @param top
     * @return
     */
    public AnimateFrameSegment location(int left, int top) {
        this.left = left;
        this.top = top;
        return this;
    }

    /**
     * 设置动画从指定序号的帧开始播放。
     *
     * @param startIndex
     * @return
     */
    public AnimateFrameSegment setStartIndex(int startIndex) {
        this.startIndex = startIndex;
        this.decodeCounter = startIndex;
        return this;
    }

    /**
     * 添加动画帧变化监听。
     *
     * @param listener
     */
    public void addOnFrameUpdateListener(OnValueChangeListener<Frame> listener) {
        if (onFrameUpdateListeners == null) {
            onFrameUpdateListeners = new LinkedList<>();
        }
        onFrameUpdateListeners.add(listener);
    }

    /**
     * 移除动画帧变化监听。
     *
     * @param listener
     */
    public void removeOnFrameUpdateListener(OnValueChangeListener<Frame> listener) {
        if (onFrameUpdateListeners != null) {
            onFrameUpdateListeners.remove(listener);
        }
    }

    private boolean isEnd() {
        return repeatCount > 0 && decodeCounter == repeatCount * source.getFrameCount();
    }

    void decode() {
        if (isActive && source.getFrameCount() > 0 && cacheQueue.size() < cacheCapacity && !isEnd()) {
            int frameCount = source.getFrameCount();
            int decodeIndex = decodeCounter % frameCount;
            if (reverse && (decodeCounter / frameCount) % 2 == 1) {
                decodeIndex = frameCount - 1 - decodeIndex;
            }
            if (decodeIndex >= 0 && decodeIndex < frameCount) {
                Frame presetFrame = presetFrames.get(decodeIndex);
                if (presetFrame == null) {
                    InputStream frameInputStream = source.getFrameInputStream(decodeIndex);
                    if (frameInputStream != null) {
                        Bitmap recycled = recycledPool.pollFirst();
                        inBitmap = recycled;
                        Bitmap decoded = BitmapFactory.decodeStream(frameInputStream, null, this);
                        if (decoded != null) {
                            cacheQueue.offer(new Frame(decoded, decodeCounter, recycledPool));
                        }
                    }
                } else {
                    cacheQueue.offer(presetFrame);
                }
            } else {
                Logger.getDefault().e("illegal index: %s(decodeCounter=%s, frameCount=%s)", decodeIndex, decodeCounter, frameCount);
            }
            decodeCounter++;
        }
    }

    /**
     * 获取动画序列帧数据源。
     *
     * @return
     */
    public AnimateFrameSource getSource() {
        return source;
    }

    @Override
    public void setPause(boolean pause) {
        this.pause = pause;
    }

    @Override
    public void prepare(boolean urgent) {
        isActive = true;
        decodeCounter = startIndex;
        decoder.enqueue(this, urgent);
    }

    @Override
    public boolean hasNext() {
        if (isActive) {
            if (pause && currentFrame != null) {
                return true;
            }
            long now = SystemClock.elapsedRealtime();
            if (refreshInterval > 0) {
                if (now - refreshTimestamp < refreshInterval) {
                    // 小于刷新间隔不需要刷新
                    return true;
                }
            }
            Frame poll = cacheQueue.poll();
            if (poll == null) {
                if (isEnd()) {
                    return fillEnd;
                } else {
                    Logger.getDefault().vv("decoding is slower than drawing: ", source, " - ", decodeCounter);
                }
            } else {
                refreshTimestamp = now;
                for (OnValueChangeListener<Frame> listener : onFrameUpdateListeners) {
                    listener.onValueChange(poll, currentFrame);
                }
                // bitmap回收利用
                if (currentFrame != null && currentFrame.isRecyclable()) {
                    Singletons.get(MainHandler.class).post(currentFrame);
                }
                currentFrame = poll;
            }
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        if (currentFrame != null) {
            canvas.drawBitmap(currentFrame.getBitmap(), left, top, null);
        }
    }

    @Override
    public void release() {
        isActive = false;
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }

    @Override
    public String toString() {
        return source + "@" + Integer.toHexString(hashCode());
    }
}

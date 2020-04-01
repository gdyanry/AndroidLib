package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import yanry.lib.android.entity.MainHandler;
import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.task.SingleThreadExecutor;
import yanry.lib.java.model.watch.ValueHolder;

/**
 * 使用序列帧实现的动画片段。具体实现上，使用线程池解码，每个处于活动状态的动画片段占用一个线程，使用队列长度为1的生产者消费者模式进行解码/绘制。
 * 功能上，支持指定播放次数、帧率、反序播放、任意帧开始等特性。
 */
public class AnimateFrameSegment implements AnimateSegment, Runnable {
    private AnimateFrameSource source;
    private SingleThreadExecutor decoder;
    private int cacheCapacity;
    private BitmapFactory.Options options;
    private ValueHolder<Frame> currentFrame;

    private Queue<Frame> cacheQueue;
    private SparseArray<Frame> presetFrames;
    private Queue<Bitmap> recycledPool;

    private boolean isActive;
    private int decodeCounter;
    private long refreshTimestamp;

    private int left;
    private int top;
    private int repeatCount;
    private boolean reverse;
    private long refreshInterval;
    private boolean pause;
    private boolean fillEnd;
    private int startIndex;
    private int zOrder;

    /**
     * @param source
     * @param decoder 序列帧解码执行线程。
     */
    public AnimateFrameSegment(@NonNull AnimateFrameSource source, @Nullable SingleThreadExecutor decoder) {
        this.source = source;
        this.decoder = decoder;
        this.cacheCapacity = 1;
        this.cacheQueue = new ConcurrentLinkedQueue<>();
        presetFrames = new SparseArray<>();
        recycledPool = new ConcurrentLinkedQueue<>();
        options = new BitmapFactory.Options();
        options.inMutable = true;
        currentFrame = new ValueHolder<>();
    }

    /**
     * 设置预解码缓存帧数，默认为1。当出现解码速度跟不上显示速度的问题时可适当调高缓存值，同时要注意避免造成非必要的内存占用。
     *
     * @param cacheCapacity
     * @return
     */
    public AnimateFrameSegment cacheCapacity(int cacheCapacity) {
        this.cacheCapacity = cacheCapacity;
        return this;
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

    private boolean isEnd() {
        return repeatCount > 0 && decodeCounter == repeatCount * source.getFrameCount();
    }

    /**
     * 获取动画序列帧数据源。
     *
     * @return
     */
    public AnimateFrameSource getSource() {
        return source;
    }

    public ValueHolder<Frame> getCurrentFrame() {
        return currentFrame;
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
            Frame previousFrame = currentFrame.getValue();
            if (pause && previousFrame != null) {
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
                    Logger.getDefault().dd("decoding is slower than drawing: ", source, " - ", decodeCounter);
                }
            } else {
                refreshTimestamp = now;
                decoder.enqueue(this, false);
                currentFrame.setValue(poll);
                // bitmap回收利用
                if (previousFrame != null && previousFrame.isRecyclable()) {
                    Singletons.get(MainHandler.class).post(previousFrame);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        Frame frame = currentFrame.getValue();
        if (frame != null) {
            canvas.drawBitmap(frame.getBitmap(), left, top, null);
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
        return source + "@" + hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnimateFrameSegment that = (AnimateFrameSegment) o;
        return left == that.left &&
                top == that.top &&
                repeatCount == that.repeatCount &&
                reverse == that.reverse &&
                refreshInterval == that.refreshInterval &&
                fillEnd == that.fillEnd &&
                startIndex == that.startIndex &&
                zOrder == that.zOrder &&
                source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, left, top, repeatCount, reverse, refreshInterval, fillEnd, startIndex, zOrder);
    }

    @Override
    public void run() {
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
                        Bitmap recycled = recycledPool.poll();
                        options.inBitmap = recycled;
                        Bitmap decoded = BitmapFactory.decodeStream(frameInputStream, null, options);
                        if (decoded != null) {
                            cacheQueue.offer(new Frame(decoded, decodeIndex, recycledPool));
                        } else {
                            Logger.getDefault().ww("decoded bitmap is null for %s on index %s", source, decodeIndex);
                        }
                    } else {
                        Logger.getDefault().ww("InputStream is null for %s on index %s", source, decodeIndex);
                    }
                } else {
                    cacheQueue.offer(presetFrame);
                }
            } else {
                Logger.getDefault().e("illegal index: %s(decodeCounter=%s, frameCount=%s)", decodeIndex, decodeCounter, frameCount);
            }
            decodeCounter++;
            if (cacheQueue.size() < cacheCapacity) {
                decoder.enqueue(this, false);
            }
        }
    }
}

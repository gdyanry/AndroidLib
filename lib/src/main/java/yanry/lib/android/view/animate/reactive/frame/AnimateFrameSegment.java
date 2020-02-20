package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.SparseArray;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import yanry.lib.android.entity.MainHandler;
import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;

/**
 * 使用序列帧实现的动画片段。具体实现上，使用线程池解码，每个处于活动状态的动画片段占用一个线程，使用队列长度为1的生产者消费者模式进行解码/绘制。
 * 功能上，支持指定播放次数、帧率、反序播放、任意帧开始等特性。
 */
public class AnimateFrameSegment implements AnimateSegment, Runnable {
    private static final int DECODE_STATE_IDLE = 0;
    private static final int DECODE_STATE_STARTING = 1;
    private static final int DECODE_STATE_STARTED = 2;
    /**
     * 当前正在解码的线程数量，记录该值用于日志输出。
     */
    private static final AtomicInteger decodingThreadCount = new AtomicInteger();

    private AnimateFrameSource source;
    private int left;
    private int top;
    private int repeatCount;
    private boolean reverse;
    private int drawCount;
    private int decodeCount;
    private int drawIndex;
    private int decodingIndex;
    private AtomicInteger activeCount;
    private ArrayBlockingQueue<Bitmap> bitmaps;
    private Bitmap frameToDraw;
    private BitmapFactory.Options options;
    private List<Bitmap> recycledPool;
    private long refreshInterval;
    private long lastDrawTime;
    private boolean pause;
    private boolean fillEnd;
    private OnFrameUpdateListener onFrameUpdateListener;
    /**
     * 用于控制解码线程的进出
     */
    private AtomicInteger decodeState;
    private SparseArray<Bitmap> presetFrames;
    private int startIndex;
    private Executor executor;
    private int zOrder;

    /**
     * @param source   序列帧数据源。
     * @param executor 用于解码序列帧的线程池。
     */
    public AnimateFrameSegment(AnimateFrameSource source, Executor executor) {
        this.source = source;
        this.executor = executor;
        bitmaps = new ArrayBlockingQueue<>(1);
        options = new BitmapFactory.Options();
        options.inMutable = true;
        recycledPool = Collections.synchronizedList(new LinkedList<Bitmap>());
        activeCount = new AtomicInteger();
        decodeState = new AtomicInteger();
        presetFrames = new SparseArray<>();
        decodingIndex = -1;
    }

    private void diagnose(LogLevel level, String msg) {
        Logger.getDefault().format(-1, level,
                "%s: %s%n(decodeState=%s,frameNum=%s,activeCount=%s,decodingIndex=%s,decodeCount=%s,drawCount=%s,repeatCount=%s,pause=%s) decodingThreadCount=%s%n%s",
                msg, source, decodeState.get(), source.getFrameCount(), activeCount.get(), decodingIndex, decodeCount, drawCount, repeatCount, pause,
                decodingThreadCount.get(), executor);
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
            presetFrames.put(index, frameBitmap);
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
        this.decodeCount = startIndex;
        this.drawCount = startIndex;
        return this;
    }

    /**
     * 设置动画帧变化监听。
     *
     * @param onFrameUpdateListener
     */
    public void setOnFrameUpdateListener(OnFrameUpdateListener onFrameUpdateListener) {
        this.onFrameUpdateListener = onFrameUpdateListener;
    }

    @Override
    public void setPause(boolean pause) {
        this.pause = pause;
    }

    @Override
    public void init() {
        activeCount.incrementAndGet();
        if (decodeState.compareAndSet(DECODE_STATE_IDLE, DECODE_STATE_STARTING)) {
            Logger.getDefault().v("init %s: %s", this, activeCount.get());
            drawCount = startIndex;
            decodeCount = startIndex;
            bitmaps.clear();
            executor.execute(this);
        }
    }

    @Override
    public boolean hasNext() {
        int frameCount = source.getFrameCount();
        boolean reachEnd = repeatCount > 0 && drawCount == repeatCount * frameCount;
        if (fillEnd && reachEnd) {
            return true;
        }
        if (activeCount.get() > 0 && source.exist() && !reachEnd) {
            if (!pause) {
                if (refreshInterval > 0) {
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastDrawTime < refreshInterval) {
                        return true;
                    }
                    lastDrawTime = now;
                }
                drawIndex = calculateIndex(drawCount);
                Bitmap frame = presetFrames.get(drawIndex);
                if (frame != null) {
                    if (onFrameUpdateListener != null) {
                        onFrameUpdateListener.onUpdateFrame(frame, drawCount, frameCount);
                    }
                    drawCount++;
                    return true;
                }
                Bitmap bitmap = bitmaps.poll();
                if (bitmap != null) {
                    if (onFrameUpdateListener != null) {
                        onFrameUpdateListener.onUpdateFrame(bitmap, drawCount, frameCount);
                    }
                    drawCount++;
                    Bitmap old = frameToDraw;
                    frameToDraw = bitmap;
                    if (old != null) {
                        Singletons.get(MainHandler.class).post(() -> recycledPool.add(old));
                    }
                } else if (decodeState.get() != DECODE_STATE_IDLE) {
                    // 解码线程被挂起，耐心等待
                    diagnose(LogLevel.Warn, "poll null");
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        Bitmap frame = presetFrames.get(drawIndex);
        if (frame != null) {
            canvas.drawBitmap(frame, left, top, null);
            return;
        }
        if (frameToDraw != null) {
            canvas.drawBitmap(frameToDraw, left, top, null);
        } else if (!pause) {
            diagnose(LogLevel.Warn, "no frame to draw");
        }
    }

    @Override
    public void release() {
        if (activeCount.decrementAndGet() == 0) {
            Logger.getDefault().v("release %s: %s", this, activeCount.get());
            onFrameUpdateListener = null;
            frameToDraw = null;
            bitmaps.clear();
            recycledPool.clear();
        }
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }

    @Override
    public String toString() {
        return source.toString();
    }

    private int calculateIndex(int count) {
        int fileNum = source.getFrameCount();
        if (fileNum <= 0) {
            Logger.getDefault().ee("invalid source: ", source);
            return 0;
        }
        int index = count % fileNum;
        if (reverse && (count / fileNum) % 2 == 1) {
            index = fileNum - 1 - index;
        }
        if (index < 0) {
            Logger.getDefault().format(1, LogLevel.Error,
                    "invalid index - %s: %s%n(decodeState=%s,frameCount=%s,activeCount=%s,decodeCount=%s,drawCount=%s,repeatCount=%s,pause=%s) runningCount=%s%n%s",
                    index, source, decodeState.get(), fileNum, activeCount.get(), decodeCount, drawCount, repeatCount, pause,
                    decodingThreadCount.get(), executor);
        }
        return index;
    }

    @Override
    public void run() {
        decodingThreadCount.incrementAndGet();
        int fileNum = source.getFrameCount();
        if (decodeState.compareAndSet(DECODE_STATE_STARTING, DECODE_STATE_STARTED)) {
            Logger.getDefault().vv("enter decoding: ", source);
            while (activeCount.get() > 0 && source.exist() && (repeatCount <= 0 || decodeCount < fileNum * repeatCount)) {
                decodingIndex = calculateIndex(decodeCount);
                if (presetFrames.get(decodingIndex) == null) {
                    InputStream frameInputStream = source.getFrameInputStream(decodingIndex);
                    if (frameInputStream != null) {
                        Bitmap recycled = null;
                        if (recycledPool.size() > 0) {
                            recycled = recycledPool.get(0);
                            if (recycled != null) {
                                recycledPool.remove(recycled);
                            }
                        }
                        try {
                            options.inBitmap = recycled;
                            Bitmap decoded = BitmapFactory.decodeStream(frameInputStream, null, options);
                            if (decoded != null) {
                                bitmaps.put(decoded);
                            }
                        } catch (InterruptedException e) {
                            Logger.getDefault().catches(e);
                        }
                    }
                }
                // 若有预置帧则跳过解码
                decodeCount++;
                decodingIndex = -1;
            }
            if (decodeState.compareAndSet(DECODE_STATE_STARTED, DECODE_STATE_IDLE)) {
                Logger.getDefault().vv("exit decoding: ", source);
            } else {
                diagnose(LogLevel.Error, "illegal decode exit state");
            }
        } else {
            diagnose(LogLevel.Error, "illegal decode enter state");
        }
        decodingThreadCount.decrementAndGet();
    }

    public interface OnFrameUpdateListener {
        void onUpdateFrame(Bitmap frame, int frameIndex, int totalNum);
    }
}

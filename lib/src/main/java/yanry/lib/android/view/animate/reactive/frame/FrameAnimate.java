package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.SparseArray;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.task.SingleThreadExecutor;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * 使用序列帧实现的动画片段。具体实现上，使用指定线程解码，多个处于活动状态的动画片段可以共用一个解码线程，使用队列长度为1的生产者消费者模式进行解码/绘制。
 * 功能上，支持指定播放次数、帧率、反序播放、任意帧开始等特性。
 */
public abstract class FrameAnimate extends AnimateSegment implements Runnable {
    static final int BMP_STATE_IDLE = 0;
    static final int BMP_STATE_IN_USE = 1;
    static final int BMP_STATE_TO_BE_IDLE = 2;

    private AnimateFrameSource source;
    private SingleThreadExecutor decoder;
    private int cacheCapacity;
    private BitmapFactory.Options options;
    private ValueHolderImpl<Frame> currentFrame;

    private Queue<Frame> cacheQueue;
    private SparseArray<Frame> presetFrames;
    private HashMap<Bitmap, AtomicInteger> bmpLock;

    private int decodeCounter;

    private int repeatCount;
    private boolean reverse;
    private boolean fillEnd;
    private int startIndex;

    /**
     * @param source
     * @param decoder 序列帧解码执行线程。
     */
    public FrameAnimate(AnimateFrameSource source, SingleThreadExecutor decoder) {
        this.source = source;
        this.decoder = decoder;
        this.cacheCapacity = 1;
        this.cacheQueue = new ConcurrentLinkedQueue<>();
        presetFrames = new SparseArray<>();
        bmpLock = new HashMap<>();
        options = new BitmapFactory.Options();
        options.inMutable = true;
        currentFrame = new ValueHolderImpl<>();
        decoder.enqueue(this, false);
    }

    /**
     * 设置预解码缓存帧数，默认为1。当出现解码速度跟不上显示速度的问题时可适当调高缓存值，同时要注意避免造成非必要的内存占用。
     *
     * @param cacheCapacity
     * @return
     */
    public FrameAnimate cacheCapacity(int cacheCapacity) {
        this.cacheCapacity = cacheCapacity;
        return this;
    }

    /**
     * @param count 动画重复播放次数，只有大于0才生效，默认无限重复播放。
     * @return
     */
    public FrameAnimate repeatCount(int count) {
        if (count == Integer.MAX_VALUE) {
            repeatCount = -1;
        } else {
            repeatCount = count;
        }
        return this;
    }

    /**
     * 预设并缓存指定序号的帧位图，通常用于缓存首帧以提高动画启动速度。
     *
     * @param index
     * @param frameBitmap
     * @return
     */
    public FrameAnimate presetFrame(int index, Bitmap frameBitmap) {
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
    public FrameAnimate reverse() {
        reverse = true;
        return this;
    }

    /**
     * 设置动画播放结束时停留在最后一帧，默认播放结束时不再显示动画。
     *
     * @return
     */
    public FrameAnimate fillEnd() {
        fillEnd = true;
        return this;
    }

    /**
     * 设置动画从指定序号的帧开始播放。
     *
     * @param startIndex
     * @return
     */
    public FrameAnimate setStartIndex(int startIndex) {
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

    /**
     * @param canvas
     * @param bitmap
     * @param index  当前帧序号。
     * @return 返回当前帧的显示时间。返回0表示一直停留在这一帧；返回-1表示结束动画。
     */
    protected abstract long drawBitmap(Canvas canvas, Bitmap bitmap, int index);

    @Override
    protected void prepare() {
        super.prepare();
        decodeCounter = startIndex;
        decoder.enqueue(this, true);
    }

    @Override
    protected final long draw(Canvas canvas) {
        Frame poll = cacheQueue.poll();
        if (poll == null) {
            if (isEnd()) {
                // 若fillEnd为true则停留在最后一帧，否则结束动画
                return fillEnd ? 0 : -1;
            } else {
                getLogger().dd("decoding is slower than drawing: ", source, " - ", decodeCounter, '/', source.getFrameCount());
            }
        } else {
            decoder.enqueue(this, false);
            Frame previousFrame = currentFrame.setValue(poll);
            if (!Objects.equals(previousFrame, poll) && previousFrame != null && previousFrame.isRecyclable()) {
                // bitmap回收利用
                bmpLock.get(previousFrame.getBitmap()).compareAndSet(BMP_STATE_IN_USE, BMP_STATE_TO_BE_IDLE);
                decoder.enqueue(previousFrame, false);
            }
        }
        Frame frame = currentFrame.getValue();
        if (frame != null) {
            int index = frame.getIndex();
            return drawBitmap(canvas, frame.getBitmap(), index);
        }
        return 16;
    }

    @Override
    public String toString() {
        return source + "@" + hashCode();
    }

    @Override
    public void run() {
        if (getAnimateState() != ANIMATE_STATE_STOPPED && source.getFrameCount() > 0 && cacheQueue.size() < cacheCapacity && !isEnd()) {
            int frameCount = source.getFrameCount();
            int decodeIndex = decodeCounter % frameCount;
            if (reverse && (decodeCounter / frameCount) % 2 == 1) {
                decodeIndex = frameCount - 1 - decodeIndex;
            }
            Frame current = currentFrame.getValue();
            if (current != null && current.getIndex() == decodeIndex) {
                cacheQueue.offer(new Frame(current.getBitmap(), decodeIndex, current.isRecyclable() ? bmpLock : null));
            } else if (decodeIndex >= 0 && decodeIndex < frameCount) {
                Frame presetFrame = presetFrames.get(decodeIndex);
                if (presetFrame == null) {
                    InputStream frameInputStream = source.getFrameInputStream(decodeIndex);
                    if (frameInputStream != null) {
                        Bitmap recycled = null;
                        for (Map.Entry<Bitmap, AtomicInteger> entry : bmpLock.entrySet()) {
                            if (entry.getValue().compareAndSet(BMP_STATE_IDLE, BMP_STATE_IN_USE)) {
                                recycled = entry.getKey();
                                break;
                            }
                        }
                        options.inBitmap = recycled;
                        Bitmap decoded = BitmapFactory.decodeStream(frameInputStream, null, options);
                        if (decoded != null) {
                            cacheQueue.offer(new Frame(decoded, decodeIndex, bmpLock));
                        } else {
                            getLogger().ww("decoded bitmap is null for %s on index %s", source, decodeIndex);
                        }
                    } else {
                        getLogger().ww("InputStream is null for %s on index %s", source, decodeIndex);
                    }
                } else {
                    cacheQueue.offer(presetFrame);
                }
            } else {
                getLogger().e("illegal index: %s(decodeCounter=%s, frameCount=%s)", decodeIndex, decodeCounter, frameCount);
            }
            decodeCounter++;
            if (cacheQueue.size() < cacheCapacity) {
                decoder.enqueue(this, false);
            }
        }
    }
}

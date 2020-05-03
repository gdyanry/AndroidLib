package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.SparseArray;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.task.SingleThreadExecutor;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * 使用序列帧实现的动画片段。具体实现上，使用指定线程解码，多个处于活动状态的动画片段可以共用一个解码线程，使用队列长度为1的生产者消费者模式进行解码/绘制。
 * 功能上，支持指定播放次数、帧率、反序播放、任意帧开始等特性。
 */
public abstract class AnimateFrameSegment extends AnimateSegment implements Runnable {
    private AnimateFrameSource source;
    private SingleThreadExecutor decoder;
    private int cacheCapacity;
    private BitmapFactory.Options options;
    private ValueHolderImpl<Frame> currentFrame;

    private Queue<Frame> cacheQueue;
    private SparseArray<Frame> presetFrames;
    private Queue<Bitmap> recycledPool;
    private Decode decode;

    private int decodeCounter;

    private int left;
    private int top;
    private int repeatCount;
    private boolean reverse;
    private boolean fillEnd;
    private int startIndex;

    /**
     * @param source
     * @param decoder 序列帧解码执行线程。
     */
    public AnimateFrameSegment(AnimateFrameSource source, SingleThreadExecutor decoder) {
        this.source = source;
        this.decoder = decoder;
        this.cacheCapacity = 1;
        this.cacheQueue = new ConcurrentLinkedQueue<>();
        presetFrames = new SparseArray<>();
        recycledPool = new LinkedList<>();
        decode = new Decode();
        options = new BitmapFactory.Options();
        options.inMutable = true;
        currentFrame = new ValueHolderImpl<>();
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

    /**
     * 获取当前帧的显示时间。
     *
     * @param frame
     * @return
     */
    protected abstract long getFrameDuration(Frame frame);

    @Override
    protected void prepare() {
        super.prepare();
        decodeCounter = startIndex;
        decoder.enqueue(decode, true);
    }

    @Override
    protected long draw(Canvas canvas) {
        Frame poll = cacheQueue.poll();
        if (poll == null) {
            if (isEnd()) {
                // 若fillEnd为true则停留在最后一帧，否则结束动画
                return fillEnd ? 0 : -1;
            } else {
                getLogger().dd("decoding is slower than drawing: ", source, " - ", decodeCounter, '/', source.getFrameCount());
            }
        } else {
            decoder.enqueue(decode, false);
            Frame previousFrame = currentFrame.getValue();
            if (currentFrame.setValue(poll) && previousFrame != null && previousFrame.isRecyclable()) {
                // bitmap回收利用
                decoder.enqueue(previousFrame, false);
            }
        }
        Frame frame = currentFrame.getValue();
        if (frame != null) {
            canvas.drawBitmap(frame.getBitmap(), left, top, null);
        }
        return getFrameDuration(frame);
    }

    @Override
    public String toString() {
        return source + "@" + hashCode();
    }

    private class Decode implements Runnable {

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
                    cacheQueue.offer(new Frame(current.getBitmap(), decodeIndex, current.isRecyclable() ? recycledPool : null));
                } else if (decodeIndex >= 0 && decodeIndex < frameCount) {
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
}

/**
 *
 */
package yanry.lib.android.entity;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import yanry.lib.java.model.log.Logger;

/**
 * @author yanry
 * <p>
 * 2016年5月22日
 */
public class DefaultExecutor extends ThreadPoolExecutor {
    private static final int THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 对于计算密集型的任务，在拥有Ncpu个处理器的系统上，当线程池的大小为Ncpu+1时，通常能实现最优的利用率。
     * （计算当计算密集型的线程偶尔由于页缺失故障或者其他原因而暂停时，这个“额外”的线程也能确保CPU的时钟周期不会被浪费）
     * <p>
     * 对于包含I/O操作或者其他阻塞操作的任务，你必须估算出任务的等待时间与计算时间的比值。
     * 这种估算不需要很精确，并且可以通过一些分析或者监控工具来获得。你还可以通过另一种方法来调节线程池的大小：在某个基准负载下，分别设置不同大小的线程池来运行应用程序，并观察CPU利用率的水平。
     * 给定如下列定义：
     * Ncpu = number of CPUs
     * Ucpu = target CPU utilization，0 <= Ucpu <= 1
     * W/C = ratio of wait time to compute time
     * 要使处理器达到期望的使用率，线程池的最优大小等于：
     * Nthreads = Ncpu * Ucpu * (1 + W/C）
     * <p>
     * queueSize * singleTaskTime / coreSize = responseTime
     */
    public DefaultExecutor() {
        super(THREAD_NUM, Integer.MAX_VALUE, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(THREAD_NUM, true), new DiscardPolicy());
        allowCoreThreadTimeOut(true);
    }

    @Override
    public final void execute(Runnable command) {
        super.execute(command);
        Logger.getDefault().ii(this);
    }
}

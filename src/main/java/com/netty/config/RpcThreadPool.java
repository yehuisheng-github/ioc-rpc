package com.netty.config;

import com.netty.annotation.ObjectScan;
import java.util.concurrent.*;

/**
 * @author yehuisheng
 */
@ObjectScan
public class RpcThreadPool extends ThreadPoolExecutor {

    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    public RpcThreadPool() {
        // 根据服务器配置和项目处理的任务复杂度，配置合适的线程池参数
        super(PROCESSORS > 2 ? PROCESSORS/3 : PROCESSORS,
                PROCESSORS*2,
                3L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(PROCESSORS * 500),
                Executors.defaultThreadFactory(),
                new SelfRejectedPolicy());
        // 设置 corePoolSize 的空闲线程超时后，也会被释放
        allowCoreThreadTimeOut(true);
    }

}

/**
 *  自定义拒绝策略
 *      线程被拒绝后，睡眠 sleepTime 毫秒，再继续尝试加入线程池的工作队列
 *      如果某线程超过 againTryAddQueueTime 毫秒还加不入线程池的工作队列，
 *      就抛弃该线程（也可以短信邮件报警、记录到日志文件中等其他处理方式）
 */
class SelfRejectedPolicy implements RejectedExecutionHandler {

    /**
     *  尝试再次进入线程池工作队列的时间（单位毫秒）
     *  如果线程超过这个时间还未进入工作队列，则抛弃当前线程
     */
    private final long againTryAddQueueTime;

    /** 被拒绝的线程睡眠多长时间后再尝试加入线程池的工作队列（单位毫秒） */
    private final long sleepTime;

    SelfRejectedPolicy() {
        // 默认 againTryAddQueueTime = 300
        this(300);
    }

    SelfRejectedPolicy(long againTryAddQueueTime) {
        // 默认 sleepTime = 50
        this(againTryAddQueueTime, 50);
    }

    SelfRejectedPolicy(long againTryAddQueueTime, long sleepTime) {
        this.againTryAddQueueTime = againTryAddQueueTime;
        this.sleepTime = sleepTime;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        Runnable runnable;
        // 判断当前线程是否被线程池拒绝过
        if (r instanceof RejectedRunnable) {
            RejectedRunnable rejectedRunnable = (RejectedRunnable) r;
            // 判断线程花费在进入工作队列的时间，有没有超出阈值
            if (rejectedRunnable.getAliveTime() > againTryAddQueueTime) {
                System.err.println("抛弃线程：" + rejectedRunnable);
                return;
            }
            runnable = r;
        } else {
            // 包装该线程，并默认记录花费在进入工作队列的时间
            runnable = new RejectedRunnable(r);
        }
        try {
            // 睡眠一段时间后，再使用线程池调用该任务
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 无论是 Runnable 还是 Callable 接口，最终都是使用 execute() 加入任务队列
        executor.execute(runnable);
    }

}

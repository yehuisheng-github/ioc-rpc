package com.netty.config;

/**
 *  包装被线程池拒绝的任务
 * @author yehuisheng
 */
public class RejectedRunnable implements Runnable {

    /** 被线程池拒绝的线程 */
    private final Runnable runnable;
    /** 当前对象的创建时间 */
    private final long createTime;

    public RejectedRunnable(Runnable runnable) {
        this.runnable = runnable;
        this.createTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        runnable.run();
    }


    /**
     * @return  获取当前对象的存活时间
     */
    public long getAliveTime() {
        return System.currentTimeMillis() - createTime;
    }

}

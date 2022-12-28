package com.netty.client;

import com.netty.annotation.ObjectScan;
import com.netty.code.MsgProtocol;
import com.netty.model.Null;
import com.netty.model.RequestMsg;
import com.netty.model.Void;
import com.netty.util.ByteUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 *
 *  当前处理器只需做这样的一件事：获取接口的请求方法、形参，将这些数据发送到服务端，
 *      然后调用 Object 的 wait 方法，线程进入等待状态，
 *      处理器接收到服务端的处理结果后，就调用 Object 的 notify 方法唤醒等待的线程，通知等待的线程将结果返回
 *
 *  @ChannelHandler.Sharable 添加注解只是标明当前 Handler 是可共享的，会在添加到 Pipeline 时去做判断，
 *                           如果 Handler 是单例，但是没有添加 Sharable 注解，Netty 就会抛异常。
 *                           Netty 并不会帮你实现单例，你添加了注解后，还需要自行将 Handler 设置为单例。
 *
 * @author yehuisheng
 */
@ObjectScan
@ChannelHandler.Sharable
public class RpcClientHandler<T> extends SimpleChannelInboundHandler<MsgProtocol> implements Supplier<T> {

    private ChannelHandlerContext channel;

    /** 请求结果 */
    private Object result;
    /** 请求对象 */
    private RequestMsg request;

    /** 加锁，Java默认非公平锁，通过fair参数可以设置，true:公平锁，false:非公平锁 */
    private final Lock lock = new ReentrantLock();
    /** 对象监视器 */
    private final Condition condition = lock.newCondition();

    public void setRequest(RequestMsg request) {
        this.request = request;
    }

    /**
     *  与服务端成功建立连接
     * @param ctx   channel上下文对象
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channel = ctx;
    }

    /**
     *  读取服务器的信息
     * @param ctx   channel上下文对象
     * @param msg   消息体
     */
    @Override
    protected synchronized void channelRead0(ChannelHandlerContext ctx, MsgProtocol msg) {
//        lock.lock();
//        try {
            this.result = ByteUtil.cast(msg.getMsg(), Object.class);
            // 接收到服务端的请求后，唤醒 get 方法继续执行
            this.notify();
//            condition.signal();
//        } finally {
//            lock.unlock();
//        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    /**
     * @return  获取服务端的远程接口的处理结果
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized T get() {
        if (request == null) {
            throw new IllegalArgumentException("找不到请求信息");
        }
        if (channel == null) {
            throw new IllegalArgumentException("服务端未开启");
        }
//        lock.lock();
        try {
            byte[] bytes = ByteUtil.getBytes(this.request);
            // 向服务端发送请求
            channel.writeAndFlush(new MsgProtocol(bytes));
            // 等待 channelRead0 响应服务端的请求结果
            this.wait();
//            condition.await();
        } catch (Exception e) {
            e.printStackTrace();
//        } finally {
//            lock.unlock();
        }
        /*
         *  无返回值或空值使用特定对象表示，
         *  如果方法无返回值，服务端也不回传消息，客户端就会一直阻塞在 Object.wait() 中
         */
        return (result instanceof Void || result instanceof Null) ? null : (T) result;
    }

}


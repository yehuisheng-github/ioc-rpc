package com.netty.client;

import com.netty.annotation.ObjectScan;
import com.netty.config.ConfigProperties;
import com.netty.annotation.AutoImport;
import com.netty.code.ProDeCoder;
import com.netty.code.ProEnCoder;
import com.netty.model.RequestMsg;
import com.netty.util.ObjectUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.lang.reflect.Proxy;

/**
 *
 *  客户端主要做两个事情：
 *      1、建立与服务端的连接
 *      2、给远程访问接口创建代理对象，当远程代理对象调用方法时，
 *        将当前接口的类型、方法和参数封装成 RequestMsg 对象，
 *        再由 clientHandler 对象携带数据访问服务端，获取接口的处理结果
 *
 * @author yehuisheng
 */
@ObjectScan
public class RpcNettyClient {

    @AutoImport
    private ConfigProperties configProperties;
    @AutoImport
    private RpcClientHandler<?> clientHandler;

    private EventLoopGroup eventLoopGroup;
    private final Object lock = new Object();

    /**
     * @param clazz     获取的接口类型
     * @param <T>       泛型
     * @return          获取接口的代理对象
     */
    public <T> T getBeanInterface(Class<T> clazz) {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("clazz不是接口类型");
        }
        // 通过 JDK 动态代理创建代理对象
        Object instance = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{clazz},
                (proxy, method, args) -> {
                    // 设置请求对象的参数
                    RequestMsg request = new RequestMsg();
                    request.setClazz(clazz);
                    request.setParamsType(method.getParameterTypes());
                    request.setMethodName(method.getName());
                    request.setParams(args);
                    /*
                     *  clientHandler 是共享对象，为了线程安全需要加锁。
                     *
                     *  这里有一个问题，clientHandler 内部使用了 synchronized + wait/notify，
                     *  wait 的方法会释放当前线程持有的 clientHandler 对象锁，
                     *  因此如果这里的 synchronized 对 clientHandler 对象加锁就会出现安全问题。
                     *
                     *  如何解决这个问题呢？
                     *      方案一： 这里的 synchronized 不锁 clientHandler 对象
                     *      方案二： clientHandler 对象内部的 wait/notify 换成 Lock 接口的 await/signal
                     */
                    synchronized (lock) {
//                    synchronized (clientHandler) {
                        clientHandler.setRequest(request);
                        return clientHandler.get();
                    }
                }
        );
        return clazz.cast(instance);
    }

    /**
     *  开启客户端，连接 netty 服务器
     */
    public void start() {
        eventLoopGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    // 设置线程池
                    .group(eventLoopGroup)
                    // 设置 NIO 通道
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            // 添加编解码器和自定义业务处理器
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ProDeCoder())
                                    .addLast(new ProEnCoder())
                                    .addLast(clientHandler);
                        }
                    });


            System.out.println("连接远程服务成功。。。");
            // 绑定服务器地址和端口号
            ChannelFuture channelFuture = bootstrap.connect(configProperties.getAddress(), configProperties.getPort());

            // 不阻塞客户端
            channelFuture.sync();
            // 阻塞客户端
//            channelFuture.channel().closeFuture().sync();

        } catch (Throwable e) {
            e.printStackTrace();
            close();
        }
    }

    public void close() {
        boolean success = false;
        if (ObjectUtil.canShutdownThreadPool(eventLoopGroup)) {
            eventLoopGroup.shutdownGracefully();
            success = true;
        }
        if (success) {
            System.out.println("客户端关闭服务了。。。");
        }
    }

}

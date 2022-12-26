package com.netty.server;

import com.netty.annotation.AutoImport;
import com.netty.annotation.ObjectScan;
import com.netty.config.ConfigProperties;
import com.netty.code.ProDeCoder;
import com.netty.code.ProEnCoder;
import com.netty.util.ObjectUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author yehuisheng
 */
@ObjectScan
public class RpcNettyServer {

    @AutoImport
    private ConfigProperties configProperties;
    @AutoImport
    private RpcServerHandler rpcServerHandler;

    private EventExecutorGroup eventExecutors;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public void start() {

        final int processors = Runtime.getRuntime().availableProcessors();

        // 负责接收客户端连接的线程池
        bossGroup = new NioEventLoopGroup(processors > 1 ? processors/2 : processors);
        // 负责IO操作/任务处理的线程池
        workerGroup = new NioEventLoopGroup(processors * 2);
        // 自定义异步任务线程组
        eventExecutors = new DefaultEventExecutorGroup(processors);

        try {
            /*
             *  1、初始化两个线程组
             *  2、设置 NIO 通信 channel
             *  3、定义阻塞队列的长度
             *  4、设置是否监控客户端的连接状态
             *  5、添加信道（channel）的处理器
             */
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // 添加编码器，解码器，handler，处理 handler 的线程池
                            pipeline.addLast(new ProDeCoder())
                                    .addLast(new ProEnCoder())
                                    .addLast(eventExecutors, rpcServerHandler);
                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind(configProperties.getPort()).sync();

            System.out.println("RPC 服务启动成功。。。");
            // 阻塞当前代码，使 netty 服务器一直处于运行状态
            channelFuture.channel().closeFuture().sync();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public void close() {
        boolean success = false;
        if (ObjectUtil.canShutdownThreadPool(bossGroup)) {
            bossGroup.shutdownGracefully();
            success = true;
        }
        if (ObjectUtil.canShutdownThreadPool(workerGroup)) {
            workerGroup.shutdownGracefully();
            success = true;
        }
        if (ObjectUtil.canShutdownThreadPool(eventExecutors)) {
            eventExecutors.shutdownGracefully();
            success = true;
        }
        if (success) {
            System.out.println("服务端关闭服务了。。。");
        }
    }

}

package com.netty.server;

import com.netty.annotation.AutoImport;
import com.netty.annotation.ObjectScan;
import com.netty.config.ConfigProperties;
import com.netty.code.MsgProtocol;
import com.netty.model.RequestMsg;
import com.netty.model.Void;
import com.netty.reflection.BeanFactory;
import com.netty.util.ByteUtil;
import com.netty.util.ObjectUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.lang.reflect.Method;

/**
 *
 *  @ChannelHandler.Sharable 添加注解只是标明当前 Handler 是可共享的，会在添加到 Pipeline 时去做判断，
 * 		                     如果 Handler 是单例，但是没有添加 Sharable 注解，Netty 就会抛异常。
 * 		                     Netty 并不会帮你实现单例，你添加了注解后，还需要自行将 Handler 设置为单例。
 *
 * @author yehuisheng
 */
@ObjectScan
@ChannelHandler.Sharable
public class RpcServerHandler extends SimpleChannelInboundHandler<MsgProtocol> {

    @AutoImport
    private ConfigProperties properties;
    @AutoImport
    private BeanFactory beanFactory;

    /**
     *  有客户端连接，就会触发该方法
     * @param ctx   channel上下文对象
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("有客户端连接了");
    }

    /**
     *  读取客户端的请求，进行处理
     * @param ctx   channel上下文对象
     * @param msg   消息体
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MsgProtocol msg) throws Exception {

        // 解析远程服务调用的请求数据
        RequestMsg request = ByteUtil.cast(msg.getMsg(), RequestMsg.class);

        // 负载均衡策略获取接口实现类
        Object instance = beanFactory.getServiceImpl(request.getClazz());

        // 获取方法和参数，执行方法得到结果
        Class<?>[] paramsType = request.getParamsType();
        Method method = ObjectUtil.isEmpty(paramsType)
                ? instance.getClass().getDeclaredMethod(request.getMethodName())
                : instance.getClass().getDeclaredMethod(request.getMethodName(), paramsType);
        Object res = method.invoke(instance, request.getParams());

        Class<?> returnType = method.getReturnType();
        // 判断有无返回值
        boolean hasReturn = !Void.class.getSimpleName().toLowerCase().equalsIgnoreCase(returnType.getName());
        // 将数据封装为自定义协议，返回客户端
        byte[] bytes = ByteUtil.getBytes(hasReturn ? res : new Void());
        ctx.writeAndFlush(new MsgProtocol(bytes));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        cause.printStackTrace();
    }

}

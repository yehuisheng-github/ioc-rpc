package com.netty.reflection;

import com.netty.annotation.EnableRpcClient;
import com.netty.annotation.EnableRpcServer;
import com.netty.config.RpcThreadPool;
import com.netty.util.ObjectUtil;

/**
 *
 *  当前类只有两个重要的方法
 *      1、run() 通过注解判断当前是客户端还是服务端，
 *        并最终调用 RpcNettyClient/RpcNettyServer 的 start 方法开启服务
 *      2、close() 关闭当前的服务和线程池
 *
 * @author yehuisheng
 */
public class Applications {

    private static BeanFactory clientBeanFactory;
    private static BeanFactory serverBeanFactory;

    /**
     *  获取方法调用者，并且判断是否有开启注解，然后运行对应的 IOC 容器
     */
    public static void run() {
        try {
            String className = new Exception().getStackTrace()[1].getClassName();
            Class<?> clazz = Class.forName(className);
            BeanFactory beanFactory;
            if (clazz.isAnnotationPresent(EnableRpcClient.class)) {
                clientBeanFactory = new ClientBeanFactory();
                beanFactory = clientBeanFactory;
            } else if (clazz.isAnnotationPresent(EnableRpcServer.class)) {
                serverBeanFactory = new ServerBeanFactory();
                beanFactory = serverBeanFactory;
            } else {
                throw new RuntimeException("请开启RPC注解功能");
            }
            // 客户端默认不阻塞，而服务端会在这个方法阻塞，监听 Channel 事件
            beanFactory.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  关闭客户端或服务端
     */
    public static void close() {
        if (serverBeanFactory != null) {
            close(serverBeanFactory);
        }
        if (clientBeanFactory != null) {
            close(clientBeanFactory);
        }
    }

    /**
     *  关闭对象工厂和线程池
     * @param beanFactory   对象工厂
     */
    private static void close(BeanFactory beanFactory) {
        RpcThreadPool threadPool = beanFactory.get(RpcThreadPool.class);
        if (ObjectUtil.canShutdownThreadPool(threadPool)) {
            threadPool.shutdown();
        }
        beanFactory.close();
    }

}

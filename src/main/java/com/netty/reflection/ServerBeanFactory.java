package com.netty.reflection;

import com.netty.server.RpcNettyServer;

/**
 * @author yehuisheng
 */
public class ServerBeanFactory extends BeanFactory {

    /**
     *  加载服务端容器
     */
    @Override
    protected void refresh() {
        if (isClose.get()) {
            synchronized (refreshLock) {
                if (isClose.get()) {
                    ReflectionFactory factory = ReflectionFactory.getInstance();
                    beansMap = factory.loadService(this);
                    beansMap.put(factory.getLowerCaseName(getClass()), this);
                    // 启动 netty 服务端
                    RpcNettyServer server = get(RpcNettyServer.class);
                    if (server != null) {
                        // 顺序不能颠倒，因为线程会阻塞在 start 方法
                        isClose.compareAndSet(true, false);
                        server.start();
                    } else {
                        throw new RuntimeException("找不到 Netty 服务器");
                    }
                }
            }
        }
    }

    @Override
    protected void close() {
        if (isClose.compareAndSet(false, true)) {
            get(RpcNettyServer.class).close();
        }
    }


}

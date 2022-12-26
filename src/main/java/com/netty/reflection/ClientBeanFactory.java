package com.netty.reflection;

import com.netty.client.RpcNettyClient;

/**
 * @author yehuisheng
 */
public class ClientBeanFactory extends BeanFactory {

    /**
     *  加载客户端容器
     */
    @Override
    protected void refresh() {
        if (isClose.get()) {
            synchronized (refreshLock) {
                if (isClose.get()) {
                    ReflectionFactory factory = ReflectionFactory.getInstance();
                    beansMap = factory.loadClient(this);
                    // 启动 netty 客户端
                    RpcNettyClient client = get(RpcNettyClient.class);
                    if (client != null) {
                        isClose.compareAndSet(true, false);
                        client.start();
                    } else {
                        throw new RuntimeException("找不到 Netty 客户端");
                    }
                }
            }
        }
    }

    @Override
    protected void close() {
        if (isClose.compareAndSet(false, true)) {
            get(RpcNettyClient.class).close();
        }
    }

}

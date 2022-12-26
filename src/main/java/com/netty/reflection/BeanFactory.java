package com.netty.reflection;

import com.netty.annotation.Service;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  容器工厂，维护容器内部的对象
 * @author yehuisheng
 */
public abstract class BeanFactory {

    protected volatile Map<String, Object> beansMap;
    protected final Object refreshLock = new Object();

    /** 当前工厂是否已关闭 */
    protected final AtomicBoolean isClose = new AtomicBoolean(true);

    /** 刷新加载工厂的对象 */
    protected abstract void refresh();

    /** 关闭工厂 */
    protected abstract void close();

    /**
     * @param clazz 类对象
     * @return      返回该类对象暴露的所有实现类
     */
    public Object getServiceImpl(Class<?> clazz) {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("该类型不是接口。。。");
        }
        List<Object> list = new ArrayList<>();
        beansMap.forEach((key, value) -> {
            // 当前类是否暴露服务，是否同类型
            if (value.getClass().isAnnotationPresent(Service.class) && clazz.isAssignableFrom(value.getClass())) {
                list.add(value);
            }
        });
        if (list.isEmpty()) {
            throw new NullPointerException("没有找到接口的实现类。。。");
        }
        if (list.size() > 1) {
            throw new NullPointerException("接口的实现类有多个。。。");
        }
        return list.get(0);
    }


    /**
     * @param beanName  对象名称
     * @param clazz     对象类型
     * @return          根据名称获取对象
     */
    public <T> T get(String beanName, Class<T> clazz) {
        Object o = get(beanName);
        try {
            return o == null ? null : clazz.cast(o);
        } catch (Exception e) {
            throw new RuntimeException("对象类型转换失败");
        }
    }


    /**
     * @param beanName  对象名称
     * @return          根据名称获取对象
     */
    public Object get(String beanName) {
        return beansMap.get(beanName);
    }


    /**
     * @param clazz     对象类型
     * @return          根据类型获取对象
     */
    public <T> T get(Class<T> clazz) {
        Set<Map.Entry<String, Object>> entrySet = beansMap.entrySet();
        List<Object> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : entrySet) {
            if (entry.getValue().getClass().equals(clazz)) {
                list.add(entry.getValue());
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return clazz.cast(list.get(0));
        }
        throw new RuntimeException("当前类型为'" + clazz + "'的对象有多个");
    }

}


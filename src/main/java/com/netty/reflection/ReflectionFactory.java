package com.netty.reflection;

import com.netty.annotation.*;
import com.netty.client.RpcNettyClient;
import org.reflections.Reflections;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 *  反射工厂
 * @author yehuisheng
 */
class ReflectionFactory {

    private final Reflections reflection;

    private ReflectionFactory() {
        File directory = new File("");
        String courseFile;
        try {
            courseFile = directory.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File file = new File(courseFile + "\\target\\classes");
        File[] files = file.listFiles();
        if (files == null) {
            throw new RuntimeException("扫描class文件失败");
        }
        // 解析 class 文件夹下的所有文件名，放入 Reflections 对象解析
        Object[] objects = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            objects[i] = files[i].getName();
        }
        this.reflection = new Reflections(objects);
    }

    private static class Inner {
        private final static ReflectionFactory FACTORY = new ReflectionFactory();
    }

    protected static ReflectionFactory getInstance() {
        return Inner.FACTORY;
    }

    /**
     * 加载服务端的对象到容器中
     * @param beanFactory   bean工厂
     */
    protected Map<String, Object> loadService(BeanFactory beanFactory) {
        Map<String, Object> map = new HashMap<>(16);
        map.put(getLowerCaseName(beanFactory.getClass()), beanFactory);
        try {
            // 扫描服务端被注解的所有对象
            Set<Class<?>> set = reflection.getTypesAnnotatedWith(EnableRpcServer.class);
            set.addAll(reflection.getTypesAnnotatedWith(ObjectScan.class));
            set.addAll(reflection.getTypesAnnotatedWith(Service.class));
            // 扫描所有注解的类
            for (Class<?> c : set) {

                // 以注解的 name 属性为 key 放到 Bean 容器中
                String name;
                if (c.isAnnotationPresent(EnableRpcServer.class)) {
                    name = c.getAnnotation(EnableRpcServer.class).name();
                } else if (c.isAnnotationPresent(Service.class)) {
                    name = c.getAnnotation(Service.class).name();
                } else {
                    name = c.getAnnotation(ObjectScan.class).name();
                }
                // name 为空则默认类名，首字母小写
                if ("".equals(name)) {
                    name = getLowerCaseName(c);
                }

                if (map.containsKey(name)) {
                    throw new RuntimeException("对象 " + name + " ，名称重复了");
                }
                // 反射创建对象存放到容器中
                map.put(name, c.getDeclaredConstructor().newInstance());

            }

            // 所有对象加载容器后，对自动注入的属性进行操作
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                // 遍历容器中对象的字段，进行自动注入
                autoWrite(map, null, entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }


    /**
     * 加载容器所有客户端自动注入的对象，并实现自动注入
     * @param beanFactory   bean工厂
     */
    protected Map<String, Object> loadClient(BeanFactory beanFactory) {
        Map<String, Object> map = new HashMap<>(16);
        map.put(getLowerCaseName(beanFactory.getClass()), beanFactory);
        try {
            // 扫描客户端被注解的所有对象
            Set<Class<?>> set = reflection.getTypesAnnotatedWith(EnableRpcClient.class);
            set.addAll(reflection.getTypesAnnotatedWith(ObjectScan.class));

            RpcNettyClient rpcNettyClient = null;

            // 扫描所有该注解的类
            for (Class<?> c : set) {

                // 以注解的 name 属性为 key 放到 Bean 容器中
                String name = c.isAnnotationPresent(EnableRpcClient.class)
                        ?  c.getAnnotation(EnableRpcClient.class).name()
                        : c.getAnnotation(ObjectScan.class).name();

                // name 为空则默认类名，首字母小写
                if ("".equals(name)) {
                    name = getLowerCaseName(c);
                }

                // 反射创建对象
                Object instance = c.getDeclaredConstructor().newInstance();
                if (instance instanceof RpcNettyClient) {
                    rpcNettyClient = (RpcNettyClient) instance;
                }

                if (map.containsKey(name)) {
                    throw new RuntimeException("对象 " + name + " ，名称重复了");
                }
                // 存放到容器中
                map.put(name, instance);

            }

            if (rpcNettyClient == null) {
                throw new RuntimeException("RpcNettyClient not found");
            } else {
                // 后面的自动注入需要调用 RpcNettyClient 的 getBean 方法，所以优先加载 RpcNettyClient
                autoWrite(map, rpcNettyClient, "rpcNettyClient", rpcNettyClient);
            }

            // 所有对象加载容器后，对自动注入的属性进行操作
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                // 遍历容器中对象的字段，进行自动注入
                autoWrite(map, rpcNettyClient, entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }


    /**
     *  给容器的对象属性进行自动赋值
     * @param map       容器
     * @param client    Rpc客户端
     * @param key       容器的 key
     * @param value     容器的对象
     */
    private void autoWrite(Map<String, Object> map, RpcNettyClient client,
                                  String key, Object value) throws Exception {
        // 遍历容器中对象的字段
        for (Field declaredField : value.getClass().getDeclaredFields()) {
            declaredField.setAccessible(true);
            // 判断字段是否自动注入
            if (client != null && declaredField.isAnnotationPresent(Reference.class)) {
                // 如果使用了 Reference 注解，就使用 client 创建代理对象
                declaredField.set(value, client.getBeanInterface(declaredField.getType()));
            } else if (declaredField.isAnnotationPresent(AutoImport.class)) {
                // 获取字段名称
                String fieldName = declaredField.getAnnotation(AutoImport.class).name();
                if ("".equals(fieldName)) {
                    fieldName = declaredField.getName();
                }
                // 容器中查找对象
                Object o = search(declaredField.getType(), map, fieldName);
                if (o == null) {
                    String s = key + "的字段'" + declaredField.getName() + "'找不到需要注入的值";
                    search(declaredField.getType(), map, fieldName);
                    throw new RuntimeException(s);
                }
                // 自动注入
                declaredField.set(value, o);
            }
        }
    }


    /**
     * @param clazz     类型
     * @param map       容器
     * @param name      字段名称
     * @return          根据类型查找容器中同类（包括父类）的对象
     */
    private Object search(Class<?> clazz, Map<String, Object> map, String name) {

        // 按名字查找
        Object o = map.get(name);
        if (o != null) {
            if (!clazz.isAssignableFrom(o.getClass())) {
                throw new RuntimeException("类名和对应的类型不一致");
            }
            return o;
        }

        // 按类型查找
        List<Object> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (clazz.isAssignableFrom(entry.getValue().getClass())) {
                list.add(entry.getValue());
            }
        }
        if (list.size() == 0) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }

        // 如果有多个同类型的对象，抛异常
        throw new RuntimeException("当前名为'" + name + "'的对象有多个");
    }

    /**
     * @param c 类对象
     * @return  返回类对象的首字母小写的名称
     */
    protected String getLowerCaseName(Class<?> c) {
        return c.getSimpleName().substring(0, 1).toLowerCase() + c.getSimpleName().substring(1);
    }

}

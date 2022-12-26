package com.netty.util;

import com.google.gson.Gson;
import com.netty.model.Null;
import java.io.*;

/**
 *  字节操作工具类
 * @author yehuisheng
 */
public class ByteUtil {

    /**
     * @param bytes     字节数组
     * @param clazz     类型
     * @param <T>       泛型转换
     * @return  将字节数组转为 Java 对象
     */
    public static <T> T cast(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            return null;
        }
        // 读取字节数组，转为 Object
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object obj = ois.readObject();
            if (obj instanceof String) {
                // 用 Gson 进行反序列化
                Gson gson = new Gson();
                return gson.fromJson(obj.toString(), clazz);
            } else {
                return clazz.cast(obj);
            }
        } catch (Exception e) {
            throw new ClassCastException("类型转换异常，" + e.getMessage());
        }
    }


    /**
     * @param obj    Object 类型的对象
     * @return      将对象转为字节数组
     */
    public static byte[] getBytes(Object obj) {
        if (obj == null) {
            obj = new Null();
        }
        if (obj instanceof Serializable) {
            // 已经序列化了
            byte[] bytes = null;
            try (ByteArrayOutputStream bo = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bo)) {
                oos.writeObject(obj);
                bytes = bo.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bytes;
        } else {
            // 使用 Gson 进行序列化
            Gson gson = new Gson();
            String json = gson.toJson(obj);
            return getBytes(json);
        }
    }

}

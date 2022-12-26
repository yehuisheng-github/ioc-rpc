package com.netty.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * @author yehuisheng
 */
public class ObjectUtil {

    /**
     * @param obj   对象
     * @return      判断对象是否为空
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof Optional) {
            return ((Optional<?>) obj).isEmpty();
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        return false;
    }


    /**
     * @param executor  线程池
     * @return          判断能否关闭线程池
     */
    public static boolean canShutdownThreadPool(ExecutorService executor) {
        return executor != null && !executor.isShutdown();
    }

}

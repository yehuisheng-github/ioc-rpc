package com.netty.model;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author yehuisheng
 */
public class Weight {

    /** 可能有多个客户端对该类型的接口进行调用 */
    private final List<String> beansList = new CopyOnWriteArrayList<>();
    /** key 存放对象名称，value 存放权值 */
    private final Map<String, Integer> weightMap;
    private final static Random RANDOM = new Random();

    public Weight(Map<String, Integer> weightMap) {
        this.weightMap = weightMap;
        reload();
    }

    /**
     *  根据权重分配bean到数组中
     */
    private void reload() {
        Set<Map.Entry<String, Integer>> entries = weightMap.entrySet();
        for (Map.Entry<String, Integer> entry : entries) {
            for (int i = 0; i <= entry.getValue(); i++) {
                beansList.add(entry.getKey());
            }
        }
    }

    /**
     * @return  从数组中随机获取beanName
     */
    public String getInstanceName() {
        if (beansList.isEmpty()) {
            reload();
        }
        return beansList.remove(RANDOM.nextInt(beansList.size()));
    }

}

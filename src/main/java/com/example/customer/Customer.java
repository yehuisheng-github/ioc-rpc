package com.example.customer;

import com.example.PersonService;
import com.netty.annotation.AutoImport;
import com.netty.annotation.EnableRpcClient;
import com.netty.annotation.Reference;
import com.netty.config.RpcThreadPool;
import com.netty.reflection.Applications;
import java.util.Random;
import java.util.UUID;

/**
 * @author yehuisheng
 */
@EnableRpcClient
public class Customer {

    @Reference
    private static PersonService personService;
    @AutoImport
    private static RpcThreadPool rpcThreadPool;

    /** 调用次数 */
    private final static int TIMES = 5000;
    private final static Random RANDOM = new Random();
    private static Integer num;

    public static void main(String[] args) {

        Applications.run();
        long millis = System.currentTimeMillis();

        // 多线程执行远程访问
        for (int i = 0; i < TIMES; i++) {
            int finalI = i;
            num = i;
            rpcThreadPool.execute(() -> {
                String name = name();
                String sex = sex();
                int age = age();
                personService.add(name, sex, age);

                name = name();
                Object person = personService.get(name);
                System.out.println(finalI + " -> call get(" + name + ") = " + person);

                name = name();
                Object remove = personService.remove(name);
                System.out.println(finalI + " -> call remove(" + name + ") = " + remove);

                System.out.println(finalI + " -> call count = " + personService.count());
                System.out.println(finalI + " -> call number(int) = " + personService.number(finalI));
                System.out.println(num + " -> call number(Integer) = " + personService.number(num));
            });
        }

        // 线程池的任务执行完毕，才执行剩下的代码
        while (rpcThreadPool.getTaskCount() != rpcThreadPool.getCompletedTaskCount()) {}

        System.err.println("交由线程池的总任务数量：" + TIMES);
        System.err.println("线程池完成的任务数量：" + rpcThreadPool.getCompletedTaskCount());
        System.err.println("线程池未完成的任务数量：" + (TIMES - rpcThreadPool.getCompletedTaskCount()));
        long time = System.currentTimeMillis() - millis;
        System.err.println("耗时：" + (time/1000) + "秒" + (time%1000) + "毫秒");

        Applications.close();

    }


    /**
     * @return  自动生成名字
     */
    private static String name() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        int nameLength = RANDOM.nextInt(6);
        return uuid.length() < nameLength ? uuid : uuid.substring(0, nameLength);
    }

    /**
     * @return  随机选择性别
     */
    private static String sex() {
        return (RANDOM.nextInt() % 2 == 0) ? "男" : "女";
    }

    /**
     * @return  随机获取年龄
     */
    private static int age() {
        return RANDOM.nextInt(100);
    }

}

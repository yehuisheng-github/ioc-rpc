package com.netty.config;

import com.netty.annotation.ObjectScan;
import java.io.*;
import java.util.Properties;

/**
 *  配置类
 * @author yehuisheng
 */
@ObjectScan
public class ConfigProperties {

    private final Properties properties;

    public ConfigProperties() {
        this.properties = new Properties();
        loadConfig();
    }

    /** 地址 */
    private String address;
    /** 端口 */
    private int port;

    /**
     *  加载配置文件
     */
    private void loadConfig() {
        File directory = new File("");
        String courseFile;
        try {
            courseFile = directory.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String path = courseFile + "\\target\\classes\\rpc.properties";
        try (FileInputStream fis = new FileInputStream(path);
             InputStream is = new BufferedInputStream(fis)) {
            properties.load(is);
        } catch (FileNotFoundException ignored) {
            // 配置文件不存在就使用默认值
        } catch (Exception e) {
            e.printStackTrace();
        }
        refreshContext();
    }


    /**
     *  刷新配置信息
     */
    private void refreshContext() {
        this.address = temple("address", "127.0.0.1", String.class);
        this.port = temple("port", 9999, Integer.class);
        System.out.println("加载配置文件，address = " + address
                + ", port = " + port);
    }


    /**
     *
     * @param fieldName     字段名
     * @param defaultValue  默认值
     * @param clazz         类型
     * @param <T>       泛型
     * @return          返回配置文件中的值，如果没有则使用默认值
     */
    private <T> T temple(String fieldName, T defaultValue, Class<T> clazz) {
        Object o = properties.get(fieldName);
        if (o != null) {
            String s = o.toString().replaceAll(" ", "");
            if ("".equals(s)) {
                return defaultValue;
            } else if (Integer.class.equals(clazz) && isNumber(s)) {
                return clazz.cast(Integer.parseInt(s));
            } else if (String.class.equals(clazz)) {
                return clazz.cast(s);
            } else {
                throw new RuntimeException("配置文件属性" + fieldName +  "类型错误，请检查");
            }
        }
        return defaultValue;
    }

    /**
     * @param value     字段值
     * @return          正则表达式判断是否整数
     */
    private boolean isNumber(String value) {
        return value.matches("-?\\d+");
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

}

package com.netty.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author yehuisheng
 */
public class RequestMsg implements Serializable {

    /** 请求参数 */
    private Object[] params;
    /** 参数类型 */
    private Class<?>[] paramsType;
    /** 请求接口 */
    private Class<?> clazz;
    /** 请求方法 */
    private String methodName;

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParamsType() {
        return paramsType;
    }

    public void setParamsType(Class<?>[] paramsType) {
        this.paramsType = paramsType;
    }

    @Override
    public String toString() {
        return "RpcRequestMsg{" +
                "params=" + Arrays.toString(params) +
                ", clazz=" + clazz +
                ", methodName=" + methodName +
                '}';
    }

}

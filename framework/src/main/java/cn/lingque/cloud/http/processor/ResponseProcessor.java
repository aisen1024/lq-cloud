package cn.lingque.cloud.http.processor;

import cn.lingque.cloud.http.bean.HttpResponseInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * 响应处理器接口
 * 用于处理HTTP响应并转换为目标类型
 * @param <T> 处理的数据类型
 */
public interface ResponseProcessor<T> {

    /**
     * 是否支持处理指定的返回类型
     * @param returnType 方法返回类型
     * @return true支持，false不支持
     */
    boolean supports(Type returnType);

    /**
     * 处理响应数据
     * @param response HTTP响应信息
     * @param method 调用的方法
     * @param returnType 方法返回类型
     * @return 处理后的结果
     */
    T processResponse(HttpResponseInfo response, Method method, Type returnType);

    /**
     * 获取处理器优先级，数值越小优先级越高
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }
}
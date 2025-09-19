package cn.lingque.cloud.http.processor;

import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.http.bean.HttpResponseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * JSON响应处理器
 * 处理JSON格式的响应数据
 */
@Slf4j
@Component
public class JsonResponseProcessor implements ResponseProcessor<Object> {

    @Override
    public boolean supports(Type returnType) {
        // 支持所有类型，作为默认处理器
        return true;
    }

    @Override
    public Object processResponse(HttpResponseInfo response, Method method, Type returnType) {
        String responseBody = response.getBody();
        
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }

        try {
            // 如果返回类型是String，直接返回
            if (returnType == String.class) {
                return responseBody;
            }

            // 如果是基本类型的包装类
            if (returnType == Integer.class || returnType == int.class) {
                return Integer.valueOf(responseBody.trim());
            }
            if (returnType == Long.class || returnType == long.class) {
                return Long.valueOf(responseBody.trim());
            }
            if (returnType == Boolean.class || returnType == boolean.class) {
                return Boolean.valueOf(responseBody.trim());
            }
            if (returnType == Double.class || returnType == double.class) {
                return Double.valueOf(responseBody.trim());
            }
            if (returnType == Float.class || returnType == float.class) {
                return Float.valueOf(responseBody.trim());
            }

            // 处理泛型类型
            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                Type rawType = parameterizedType.getRawType();
                
                // 处理常见的泛型响应格式，如 ApiResult<T>
                if (rawType instanceof Class) {
                    Class<?> rawClass = (Class<?>) rawType;
                    return JSONUtil.toBean(responseBody, rawClass);
                }
            }

            // 处理普通的类类型
            if (returnType instanceof Class) {
                Class<?> clazz = (Class<?>) returnType;
                
                // 如果是Map类型
                if (Map.class.isAssignableFrom(clazz)) {
                    return JSONUtil.parseObj(responseBody);
                }
                
                // 其他对象类型
                return JSONUtil.toBean(responseBody, clazz);
            }

            // 默认解析为JSONObject
            return JSONUtil.parseObj(responseBody);
            
        } catch (Exception e) {
            log.error("Failed to process response: {}", responseBody, e);
            throw new RuntimeException("Failed to process response", e);
        }
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE; // 最低优先级，作为默认处理器
    }
}
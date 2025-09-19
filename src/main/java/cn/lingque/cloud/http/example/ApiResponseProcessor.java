package cn.lingque.cloud.http.example;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.http.bean.HttpResponseInfo;
import cn.lingque.cloud.http.processor.ResponseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * API响应处理器示例
 * 处理标准的API响应格式，如：
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {...}
 * }
 */
@Slf4j
@Component
public class ApiResponseProcessor implements ResponseProcessor<Object> {

    @Override
    public boolean supports(Type returnType) {
        // 支持ApiResult<T>类型的响应
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            Type rawType = parameterizedType.getRawType();
            
            if (rawType instanceof Class) {
                Class<?> rawClass = (Class<?>) rawType;
                return "ApiResult".equals(rawClass.getSimpleName()) || 
                       "Result".equals(rawClass.getSimpleName()) ||
                       "Response".equals(rawClass.getSimpleName());
            }
        }
        return false;
    }

    @Override
    public Object processResponse(HttpResponseInfo response, Method method, Type returnType) {
        String responseBody = response.getBody();
        
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return createErrorResult("Empty response body");
        }

        try {
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            
            // 检查响应格式
            if (!jsonResponse.containsKey("code")) {
                log.warn("Response does not contain 'code' field: {}", responseBody);
                return createErrorResult("Invalid response format");
            }

            Integer code = jsonResponse.getInt("code");
            String message = jsonResponse.getStr("message", "Unknown");
            Object data = jsonResponse.get("data");

            // 检查业务状态码
            if (code == null || code != 200) {
                log.warn("API returned error code: {}, message: {}", code, message);
                return createErrorResult(message, code);
            }

            // 处理泛型类型
            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                
                if (actualTypeArguments.length > 0) {
                    Type dataType = actualTypeArguments[0];
                    
                    // 转换data字段到目标类型
                    Object convertedData = convertData(data, dataType);
                    return createSuccessResult(convertedData, message);
                }
            }

            return createSuccessResult(data, message);
            
        } catch (Exception e) {
            log.error("Failed to process API response: {}", responseBody, e);
            return createErrorResult("Failed to parse response: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return -50; // 较高优先级
    }

    /**
     * 转换数据到目标类型
     */
    private Object convertData(Object data, Type targetType) {
        if (data == null) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return data.toString();
            }
            
            if (targetType instanceof Class) {
                Class<?> targetClass = (Class<?>) targetType;
                
                // 基本类型处理
                if (targetClass.isPrimitive() || 
                    Number.class.isAssignableFrom(targetClass) ||
                    Boolean.class.isAssignableFrom(targetClass)) {
                    return data;
                }
                
                // 对象类型转换
                if (data instanceof JSONObject) {
                    return ((JSONObject) data).toBean(targetClass);
                } else {
                    return JSONUtil.toBean(JSONUtil.toJsonStr(data), targetClass);
                }
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("Failed to convert data to type: {}", targetType, e);
            return data;
        }
    }

    /**
     * 创建成功结果
     */
    private Object createSuccessResult(Object data, String message) {
        // 这里返回一个简单的结果对象
        // 实际项目中可能需要返回具体的ApiResult对象
        return new SimpleApiResult(200, message, data, true);
    }

    /**
     * 创建错误结果
     */
    private Object createErrorResult(String message) {
        return createErrorResult(message, 500);
    }

    /**
     * 创建错误结果
     */
    private Object createErrorResult(String message, Integer code) {
        return new SimpleApiResult(code != null ? code : 500, message, null, false);
    }

    /**
     * 简单的API结果对象
     */
    public static class SimpleApiResult {
        private final Integer code;
        private final String message;
        private final Object data;
        private final Boolean success;

        public SimpleApiResult(Integer code, String message, Object data, Boolean success) {
            this.code = code;
            this.message = message;
            this.data = data;
            this.success = success;
        }

        public Integer getCode() { return code; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
        public Boolean getSuccess() { return success; }

        @Override
        public String toString() {
            return String.format("ApiResult{code=%d, message='%s', success=%s, data=%s}", 
                code, message, success, data);
        }
    }
}
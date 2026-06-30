package cn.lingque.cloud.http.processor;

import cn.lingque.cloud.http.bean.HttpResponseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 响应处理器管理器
 * 负责管理和执行所有的响应处理器
 */
@Slf4j
@Component
public class ResponseProcessorManager {

    private final List<ResponseProcessor<?>> processors;

    @Autowired
    public ResponseProcessorManager(List<ResponseProcessor<?>> processors) {
        this.processors = new ArrayList<>(processors);
        // 按优先级排序
        this.processors.sort(Comparator.comparingInt(ResponseProcessor::getOrder));
        
        log.info("Loaded {} response processors", this.processors.size());
        for (ResponseProcessor<?> processor : this.processors) {
            log.debug("Response processor: {} with order: {}", 
                processor.getClass().getSimpleName(), processor.getOrder());
        }
    }

    /**
     * 处理响应数据
     * @param response HTTP响应信息
     * @param method 调用的方法
     * @param returnType 方法返回类型
     * @return 处理后的结果
     */
    @SuppressWarnings("unchecked")
    public Object processResponse(HttpResponseInfo response, Method method, Type returnType) {
        for (ResponseProcessor<?> processor : processors) {
            if (processor.supports(returnType)) {
                try {
                    log.debug("Processing response with: {}", processor.getClass().getSimpleName());
                    return ((ResponseProcessor<Object>) processor).processResponse(response, method, returnType);
                } catch (Exception e) {
                    log.error("Error processing response with processor: {}", 
                        processor.getClass().getSimpleName(), e);
                    throw new RuntimeException("Failed to process response", e);
                }
            }
        }
        
        // 如果没有找到合适的处理器，返回原始响应体
        log.warn("No suitable response processor found for return type: {}", returnType);
        return response.getBody();
    }
}
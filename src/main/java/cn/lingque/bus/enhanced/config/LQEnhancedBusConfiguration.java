package cn.lingque.bus.enhanced.config;

import cn.lingque.bus.enhanced.BusSubscriber;
import cn.lingque.bus.enhanced.EnhancedBusMessage;
import cn.lingque.bus.enhanced.LQEnhancedBus;
import cn.lingque.bus.enhanced.annotation.LQEnhancedBusListener;
import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.util.LQUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强版消息总线配置类
 * 自动扫描和注册带有@LQEnhancedBusListener注解的方法
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
public class LQEnhancedBusConfiguration implements BeanPostProcessor {
    
    /** SpEL表达式解析器 */
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    /** 已解析的表达式缓存 */
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    
    /** 重试计数器 */
    private final Map<String, Integer> retryCounters = new ConcurrentHashMap<>();
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 扫描带有@LQEnhancedBusListener注解的方法
        Map<Method, LQEnhancedBusListener> annotatedMethods = MethodIntrospector.selectMethods(
                bean.getClass(),
                (MethodIntrospector.MetadataLookup<LQEnhancedBusListener>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, LQEnhancedBusListener.class)
        );
        
        for (Map.Entry<Method, LQEnhancedBusListener> entry : annotatedMethods.entrySet()) {
            Method method = entry.getKey();
            LQEnhancedBusListener listener = entry.getValue();
            
            if (listener.enabled()) {
                registerBusListener(bean, method, listener);
            }
        }

        //启动bus
        startEnhancedBus();
        
        return bean;
    }
    
    /**
     * 注册消息总线监听器
     */
    private void registerBusListener(Object bean, Method method, LQEnhancedBusListener listener) {
        // 检查方法参数
        if (method.getParameterCount() != 1 && method.getParameterCount() != 2) {
            throw new IllegalStateException("Method " + method.getName() + 
                    " must have one parameter (message) or two parameters (message, busMessage)");
        }
        
        Class<?> messageType = method.getParameterTypes()[0];
        String subscriberName = LQUtil.isEmpty(listener.name()) ? 
                method.getName() : listener.name();
        
        // 创建订阅者
        BusSubscriber<Object> subscriber = new BusSubscriber<Object>() {
            @Override
            public void onMessage(Object message, EnhancedBusMessage busMessage) {
                try {
                    // 检查过滤条件
                    if (!checkCondition(message, busMessage, listener.condition())) {
                        log.debug("消息被过滤: topic={}, condition={}", listener.topic(), listener.condition());
                        return;
                    }
                    
                    // 调用方法
                    if (method.getParameterCount() == 1) {
                        method.invoke(bean, message);
                    } else {
                        method.invoke(bean, message, busMessage);
                    }
                    
                    // 重置重试计数器
                    String retryKey = getRetryKey(bean, method, busMessage);
                    retryCounters.remove(retryKey);
                    
                    log.debug("消息处理成功: topic={}, subscriber={}, messageId={}", 
                            listener.topic(), subscriberName, busMessage.getMessageId());
                    
                } catch (Exception e) {
                    handleError(bean, method, listener, message, busMessage, e);
                }
            }
            
            @Override
            public Class<Object> getMessageClass() {
                return (Class<Object>) messageType;
            }
            
            @Override
            public String getSubscriberName() {
                return subscriberName;
            }
            
            @Override
            public boolean isAsync() {
                return listener.async();
            }
            
            @Override
            public int getPriority() {
                return listener.priority();
            }
        };
        
        // 注册到消息总线
        String serviceGroup = LQUtil.isEmpty(listener.serviceGroup()) ? null : listener.serviceGroup();
        LQEnhancedBus.subscribe(listener.topic(), serviceGroup, subscriber);
        
        log.info("注册增强版消息总线监听器: topic={}, serviceGroup={}, subscriber={}, class={}, method={}",
                listener.topic(), serviceGroup, subscriberName, bean.getClass().getSimpleName(), method.getName());
    }
    
    /**
     * 检查过滤条件
     */
    private boolean checkCondition(Object message, EnhancedBusMessage busMessage, String condition) {
        if (LQUtil.isEmpty(condition)) {
            return true;
        }
        
        try {
            Expression expression = expressionCache.computeIfAbsent(condition, 
                    expressionParser::parseExpression);
            
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("message", message);
            context.setVariable("busMessage", busMessage);
            
            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;
            
        } catch (Exception e) {
            log.warn("过滤条件执行失败: condition={}, error={}", condition, e.getMessage());
            return true; // 条件执行失败时默认通过
        }
    }
    
    /**
     * 处理错误
     */
    private void handleError(Object bean, Method method, LQEnhancedBusListener listener, 
                           Object message, EnhancedBusMessage busMessage, Exception error) {
        String errorMsg = String.format("消息处理失败: topic=%s, subscriber=%s, messageId=%s", 
                listener.topic(), listener.name(), busMessage.getMessageId());
        
        switch (listener.errorStrategy()) {
            case LOG:
                log.error(errorMsg, error);
                break;
                
            case RETRY:
                handleRetry(bean, method, listener, message, busMessage, error);
                break;
                
            case IGNORE:
                log.debug(errorMsg + ", 错误被忽略", error);
                break;
                
            case THROW:
                log.error(errorMsg, error);
                throw new RuntimeException(errorMsg, error);
                
            default:
                log.error(errorMsg, error);
        }
    }
    
    /**
     * 处理重试
     */
    private void handleRetry(Object bean, Method method, LQEnhancedBusListener listener, 
                           Object message, EnhancedBusMessage busMessage, Exception error) {
        String retryKey = getRetryKey(bean, method, busMessage);
        int currentRetries = retryCounters.getOrDefault(retryKey, 0);
        
        if (currentRetries < listener.maxRetries()) {
            retryCounters.put(retryKey, currentRetries + 1);
            
            log.warn("消息处理失败，准备重试: topic={}, subscriber={}, messageId={}, retry={}/{}", 
                    listener.topic(), listener.name(), busMessage.getMessageId(), 
                    currentRetries + 1, listener.maxRetries(), error);
            
            // 延迟重试
            new Thread(() -> {
                try {
                    Thread.sleep(listener.retryInterval());
                    
                    // 重新处理消息
                    if (method.getParameterCount() == 1) {
                        method.invoke(bean, message);
                    } else {
                        method.invoke(bean, message, busMessage);
                    }
                    
                    // 重试成功，清除计数器
                    retryCounters.remove(retryKey);
                    
                    log.info("消息重试处理成功: topic={}, subscriber={}, messageId={}, retry={}", 
                            listener.topic(), listener.name(), busMessage.getMessageId(), currentRetries + 1);
                    
                } catch (Exception retryError) {
                    // 递归重试
                    handleRetry(bean, method, listener, message, busMessage, retryError);
                }
            }).start();
            
        } else {
            // 重试次数用完，记录错误
            retryCounters.remove(retryKey);
            log.error("消息处理重试失败，已达到最大重试次数: topic={}, subscriber={}, messageId={}, maxRetries={}", 
                    listener.topic(), listener.name(), busMessage.getMessageId(), listener.maxRetries(), error);
        }
    }
    
    /**
     * 获取重试键
     */
    private String getRetryKey(Object bean, Method method, EnhancedBusMessage busMessage) {
        return bean.getClass().getName() + "#" + method.getName() + "#" + busMessage.getMessageId();
    }
    
    /**
     * 启动增强版消息总线
     * 在Spring容器启动完成后自动调用
     */
    public void startEnhancedBus() {
        try {
            // 获取当前节点信息
            LQEnhancedNodeInfo currentNode = getCurrentNodeInfo();
            
            if (currentNode != null) {
                // 启动增强版消息总线
                LQEnhancedBus.start(currentNode);
                log.info("增强版消息总线启动成功: {}", currentNode.getServerName());
            } else {
                log.warn("无法获取当前节点信息，增强版消息总线启动失败");
            }
            
        } catch (Exception e) {
            log.error("启动增强版消息总线失败", e);
        }
    }
    
    /**
     * 获取当前节点信息
     */
    private LQEnhancedNodeInfo getCurrentNodeInfo() {
        // 从注册中心获取当前节点信息
        if (!LQEnhancedRegisterCenter.currentEnhancedNodes.isEmpty()) {
            return LQEnhancedRegisterCenter.currentEnhancedNodes.iterator().next();
        }
        
        // 如果没有注册的增强节点，创建一个默认节点
        return new LQEnhancedNodeInfo()
                .setServerName("enhanced-bus-node")
                .setNodeIp("127.0.0.1")
                .setNodePort(8080)
                .setProtocol("HTTP")
                .setWeight(100)
                .setVersion("1.0.0")
                .addTag("bus")
                .addTag("enhanced");
    }
}
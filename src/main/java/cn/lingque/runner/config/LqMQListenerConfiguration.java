package cn.lingque.runner.config;

import cn.lingque.base.LQKey;
import cn.lingque.mq.LQMQConsumer;
import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.runner.annon.LqMQListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Configuration
public class LqMQListenerConfiguration implements BeanPostProcessor, ApplicationContextAware {
    
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Map<Method, LqMQListener> annotatedMethods = MethodIntrospector.selectMethods(
            bean.getClass(),
            (MethodIntrospector.MetadataLookup<LqMQListener>) method ->
                AnnotatedElementUtils.findMergedAnnotation(method, LqMQListener.class)
        );

        for (Map.Entry<Method, LqMQListener> entry : annotatedMethods.entrySet()) {
            Method method = entry.getKey();
            LqMQListener listener = entry.getValue();
            registerMQListener(bean, method, listener);
        }

        return bean;
    }

    private void registerMQListener(Object bean, Method method, LqMQListener listener) {
        // 检查方法参数
        if (method.getParameterCount() != 1) {
            throw new IllegalStateException("Method " + method.getName() + " must have exactly one parameter");
        }

        Class<?> parameterType = method.getParameterTypes()[0];
        String key = LQKey.key(listener.key(),listener.version(),LQKey.ONE_DAY).buildKey();

        // 创建消息处理器
        ILQMessage<?> messageHandler = new ILQMessage<Object>() {
            @Override
            public void handle(Object message) {
                try {
                    method.invoke(bean, message);
                } catch (Exception e) {
                    log.error("Error processing message for key: " + key, e);
                }
            }

            @Override
            public Class getEntityClass() {
                return parameterType;
            }
        };

        // 根据不同的队列类型注册处理器
        LingQueRedis redis = LingQueRedis.ofKey(key, LQKey.HALF_DAY);
        switch (listener.type()) {
            case SEQUENCE:
                log.warn("SEQUENCE类型队列已弃用，建议使用UNIFIED替代。队列key: {}", key);
                LQMQConsumer.register(key, redis.ofSequenceQueue(), messageHandler);
                break;
            case LAZY:
                log.warn("LAZY类型队列已弃用，建议使用UNIFIED替代。队列key: {}", key);
                LQMQConsumer.register(key, redis.ofLazyQueue(), messageHandler);
                break;
            case UNIQUE:
                log.warn("UNIQUE类型队列已弃用，建议使用UNIFIED替代。队列key: {}", key);
                LQMQConsumer.register(key, redis.ofUniqueQueue(), messageHandler);
                break;
            case UNIFIED:
                // 不再使用LQMQConsumer的轮询机制，直接启动队列自己的监听器
                redis.ofUnifiedQueue().consumer(java.util.Arrays.asList(messageHandler));
                break;
            case STREAM_UNIFIED:
                // 不再使用LQMQConsumer的轮询机制，直接启动队列自己的监听器
                redis.ofStreamUnifiedQueue().consumer(java.util.Arrays.asList(messageHandler));
                break;
        }

        log.info("Registered MQ listener for key: {}, type: {}, method: {}", 
            key, listener.type(), method.getName());
    }
} 
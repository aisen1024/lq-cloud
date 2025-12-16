package cn.lingque.runner.config;

import cn.lingque.base.LQKey;
import cn.lingque.config.LQCloudAutoConfiguration;
import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.runner.LqCloudRunner;
import cn.lingque.runner.annon.LqMQListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * MQ监听器配置类
 * 必须在LqCloudRunner初始化后加载，确保线程池和Redis已就绪
 */
@Slf4j
@Configuration
@AutoConfigureAfter(LQCloudAutoConfiguration.class)
@ConditionalOnBean(LqCloudRunner.class)
@DependsOn("lqCloudRunner")
public class LqMQListenerConfiguration implements BeanPostProcessor, ApplicationContextAware {
    
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        log.debug("MQ监听器配置已加载");
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
        LingQueRedis redis = LingQueRedis.ofKey(key, LQKey.ONE_DAY);
        redis.ofUnifiedQueue().consumer(java.util.Arrays.asList(messageHandler));
        redis.ofStreamUnifiedQueue().consumer(java.util.Arrays.asList(messageHandler));

        log.info("Registered MQ listener for key: {}, method: {}",
            key, method.getName());
    }
} 
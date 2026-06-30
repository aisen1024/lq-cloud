package cn.lingque.runner.config;

import cn.lingque.bus.enhanced.BusSubscriber;
import cn.lingque.bus.enhanced.EnhancedBusMessage;
import cn.lingque.bus.enhanced.LQEnhancedBus;
import cn.lingque.bus.enhanced.annotation.LQEnhancedBusListener;
import cn.lingque.config.LQCloudAutoConfiguration;
import cn.lingque.runner.LqCloudRunner;
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
 * 增强版消息总线配置类
 * 扫描 @LQEnhancedBusListener 注解，自动注册到 LQEnhancedBus
 */
@Slf4j
@Configuration
@AutoConfigureAfter(LQCloudAutoConfiguration.class)
@ConditionalOnBean(LqCloudRunner.class)
@DependsOn("lqCloudRunner")
public class LqBusConfiguration implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        log.debug("增强版消息总线配置已加载");
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
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

        return bean;
    }

    private void registerBusListener(Object bean, Method method, LQEnhancedBusListener listener) {
        int paramCount = method.getParameterCount();
        if (paramCount < 1 || paramCount > 2) {
            throw new IllegalStateException(
                "Method " + method.getName() + " must have 1 or 2 parameters: (MessageType) or (MessageType, EnhancedBusMessage)"
            );
        }

        Class<?> messageType = method.getParameterTypes()[0];
        String topic = listener.topic();
        String serviceGroup = listener.serviceGroup().isEmpty() ? null : listener.serviceGroup();
        String subscriberName = listener.name().isEmpty() ? method.getName() : listener.name();
        boolean async = listener.async();
        int priority = listener.priority();

        @SuppressWarnings("unchecked")
        BusSubscriber<Object> subscriber = new BusSubscriber<Object>() {
            @Override
            public void onMessage(Object message, EnhancedBusMessage busMessage) {
                try {
                    if (paramCount == 2) {
                        method.invoke(bean, message, busMessage);
                    } else {
                        method.invoke(bean, message);
                    }
                } catch (Exception e) {
                    log.error("处理总线消息失败: topic={}, method={}", topic, method.getName(), e);
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<Object> getMessageClass() {
                return (Class<Object>) messageType;
            }

            @Override
            public String getSubscriberName() {
                return subscriberName;
            }

            @Override
            public boolean isAsync() {
                return async;
            }

            @Override
            public int getPriority() {
                return priority;
            }
        };

        LQEnhancedBus.subscribe(topic, serviceGroup, subscriber);

        log.info("注册总线监听器: topic={}, serviceGroup={}, method={}, async={}",
            topic, serviceGroup, method.getName(), async);
    }
}

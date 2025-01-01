package cn.lingque.runner.config;

import cn.lingque.base.LQKey;
import cn.lingque.bus.BusHandleBeanInfo;
import cn.lingque.bus.LQBus;
import cn.lingque.mq.LQMQConsumer;
import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.runner.annon.LqEvenBus;
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
public class LqBusConfiguration implements BeanPostProcessor, ApplicationContextAware {
    
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Map<Method, LqEvenBus> annotatedMethods = MethodIntrospector.selectMethods(
            bean.getClass(),
            (MethodIntrospector.MetadataLookup<LqEvenBus>) method ->
                AnnotatedElementUtils.findMergedAnnotation(method, LqEvenBus.class)
        );

        for (Map.Entry<Method, LqEvenBus> entry : annotatedMethods.entrySet()) {
            Method method = entry.getKey();
            LqEvenBus listener = entry.getValue();
            registerBusListener(bean, method, listener);
        }

        return bean;
    }

    private void registerBusListener(Object bean, Method method, LqEvenBus listener) {
        // 检查方法参数
        if (method.getParameterCount() != 1) {
            throw new IllegalStateException("Method " + method.getName() + " must have exactly one parameter");
        }
        Class<?> parameterType = method.getParameterTypes()[0];
        String key = listener.even();
        BusHandleBeanInfo info = new BusHandleBeanInfo(parameterType,(data)->{
            try {
                method.invoke(bean, data);
            } catch (Exception e) {
                log.error("Error processing message for key: " + key, e);
            }
        });
        LQBus.registerBus(key,info);
        log.info("Registered Bus even for key: {}, type: {}, method: {}",
            key, listener.even(), method.getName());
    }
} 
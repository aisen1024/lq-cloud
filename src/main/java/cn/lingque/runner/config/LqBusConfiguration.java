package cn.lingque.runner.config;

import cn.lingque.bus.BusHandle;
import cn.lingque.bus.BusHandleBeanInfo;
import cn.lingque.bus.LQBus;
import cn.lingque.config.LQCloudAutoConfiguration;
import cn.lingque.runner.LqCloudRunner;
import cn.lingque.runner.annon.LqEvenBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 消息总线配置类
 * 必须在LqCloudRunner初始化后加载，确保消息总线已启动
 */
@Configuration
@AutoConfigureAfter(LQCloudAutoConfiguration.class)
@ConditionalOnBean(LqCloudRunner.class)
@DependsOn("lqCloudRunner")
public class LqBusConfiguration implements BeanPostProcessor, ApplicationContextAware {
    
    private static final Logger log = LoggerFactory.getLogger(LqBusConfiguration.class);
    
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        log.debug("消息总线配置已加载");
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
        BusHandleBeanInfo info = new BusHandleBeanInfo(parameterType, (data) -> {
            try {
                method.invoke(bean, data);
            } catch (Exception e) {
                log.error("Error processing message for key: " + key, e);
            }
        });
        LQBus.registerBus(key, info);
        log.info("Registered Bus event for key: {}, type: {}, method: {}",
            key, listener.even(), method.getName());
    }
} 
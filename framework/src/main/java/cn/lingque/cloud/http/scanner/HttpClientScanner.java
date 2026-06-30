package cn.lingque.cloud.http.scanner;

import cn.lingque.cloud.http.annotation.HttpClient;
import cn.lingque.cloud.http.proxy.HttpClientProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * HTTP客户端扫描器
 * 自动扫描标注了@HttpClient的接口并注册为Spring Bean
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
public class HttpClientScanner implements BeanDefinitionRegistryPostProcessor {
    
    private static final String BASE_PACKAGE = "cn.lingque";
    
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        scanHttpClients(registry);
    }
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 不需要实现
    }
    
    /**
     * 扫描HTTP客户端接口
     */
    private void scanHttpClients(BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);
        
        // 添加@HttpClient注解过滤器
        scanner.addIncludeFilter(new AnnotationTypeFilter(HttpClient.class));
        
        // 扫描指定包下的类
        Set<org.springframework.beans.factory.config.BeanDefinition> candidates = 
                scanner.findCandidateComponents(BASE_PACKAGE);
        
        for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
            try {
                String className = candidate.getBeanClassName();
                Class<?> clazz = Class.forName(className);
                
                if (clazz.isInterface() && clazz.isAnnotationPresent(HttpClient.class)) {
                    registerHttpClient(registry, clazz);
                }
            } catch (Exception e) {
                log.error("注册HTTP客户端失败: {}", candidate.getBeanClassName(), e);
            }
        }
    }
    
    /**
     * 注册HTTP客户端
     */
    private void registerHttpClient(BeanDefinitionRegistry registry, Class<?> interfaceClass) {
        String beanName = generateBeanName(interfaceClass);
        
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HttpClientFactoryBean.class);
        builder.addPropertyValue("interfaceClass", interfaceClass);
        builder.setLazyInit(true);
        
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
        
        log.info("注册HTTP客户端: {} -> {}", interfaceClass.getName(), beanName);
    }
    
    /**
     * 生成Bean名称
     */
    private String generateBeanName(Class<?> interfaceClass) {
        String simpleName = interfaceClass.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
    
    /**
     * HTTP客户端工厂Bean
     */
    public static class HttpClientFactoryBean implements org.springframework.beans.factory.FactoryBean<Object> {
        
        private Class<?> interfaceClass;
        
        public void setInterfaceClass(Class<?> interfaceClass) {
            this.interfaceClass = interfaceClass;
        }
        
        @Override
        public Object getObject() throws Exception {
            return HttpClientProxyFactory.createProxy(interfaceClass);
        }
        
        @Override
        public Class<?> getObjectType() {
            return interfaceClass;
        }
        
        @Override
        public boolean isSingleton() {
            return true;
        }
    }
}
package cn.lingque.cloud.config.enhanced;

import cn.lingque.bus.enhanced.LQEnhancedBus;
import cn.lingque.util.LQUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LQ配置刷新管理器
 * 支持@Value注解的动态配置刷新
 * 
 * @author LingQue
 * @version 1.0
 */
@Component
public class LQConfigRefreshManager implements BeanPostProcessor, ApplicationContextAware {
    
    private static final String LOG_PREFIX = "[LQConfigRefresh]";
    
    // Spring表达式模式匹配
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    // 存储需要刷新的字段信息
    private final Map<String, List<RefreshableField>> configFieldMap = new ConcurrentHashMap<>();
    
    // 存储Bean实例
    private final Map<String, Object> beanInstanceMap = new ConcurrentHashMap<>();
    
    private ApplicationContext applicationContext;
    private Environment environment;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.environment = applicationContext.getEnvironment();
    }
    
    @PostConstruct
    public void init() {
        System.out.println(LOG_PREFIX + " 配置刷新管理器初始化完成");
        
        // 注册配置变更监听器
        LQEnhancedConfigCenter.addGlobalConfigListener(this::handleConfigChange);
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 扫描Bean中的@Value注解字段
        scanValueAnnotations(bean, beanName);
        return bean;
    }
    
    /**
     * 扫描Bean中的@Value注解字段
     */
    private void scanValueAnnotations(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();
        
        // 遍历所有字段
        ReflectionUtils.doWithFields(clazz, field -> {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                String expression = valueAnnotation.value();
                Set<String> configKeys = extractConfigKeys(expression);
                
                if (!configKeys.isEmpty()) {
                    // 存储Bean实例
                    beanInstanceMap.put(beanName, bean);
                    
                    // 为每个配置键注册刷新字段
                    for (String configKey : configKeys) {
                        RefreshableField refreshableField = new RefreshableField(
                            beanName, bean, field, expression, configKey
                        );
                        
                        configFieldMap.computeIfAbsent(configKey, k -> new ArrayList<>())
                                     .add(refreshableField);
                        
                        System.out.println(LOG_PREFIX + " 注册配置刷新字段: " + 
                                         beanName + "." + field.getName() + " -> " + configKey);
                    }
                }
            }
        });
    }
    
    /**
     * 从Spring表达式中提取配置键
     */
    private Set<String> extractConfigKeys(String expression) {
        Set<String> configKeys = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            // 处理默认值 key:defaultValue
            String configKey = placeholder.split(":")[0].trim();
            configKeys.add(configKey);
        }
        
        return configKeys;
    }
    
    /**
     * 处理配置变更事件
     */
    private void handleConfigChange(ConfigChangeEvent event) {
        String fullConfigKey = event.getFullConfigKey();
        String configKey = event.getConfigKey();
        
        // 查找需要刷新的字段
        List<RefreshableField> fieldsToRefresh = new ArrayList<>();
        
        // 精确匹配
        List<RefreshableField> exactMatches = configFieldMap.get(fullConfigKey);
        if (exactMatches != null) {
            fieldsToRefresh.addAll(exactMatches);
        }
        
        // 如果没有命名空间，也检查配置键
        if (!fullConfigKey.equals(configKey)) {
            List<RefreshableField> keyMatches = configFieldMap.get(configKey);
            if (keyMatches != null) {
                fieldsToRefresh.addAll(keyMatches);
            }
        }
        
        if (!fieldsToRefresh.isEmpty()) {
            System.out.println(LOG_PREFIX + " 配置变更触发字段刷新: " + fullConfigKey + 
                             " -> " + fieldsToRefresh.size() + " 个字段");
            
            // 刷新字段值
            for (RefreshableField refreshableField : fieldsToRefresh) {
                refreshField(refreshableField, event);
            }
            
            // 发送刷新完成事件
            Map<String, Object> refreshData = new HashMap<>();
            refreshData.put("configKey", fullConfigKey);
            refreshData.put("refreshedFields", fieldsToRefresh.size());
            refreshData.put("timestamp", System.currentTimeMillis());
            LQEnhancedBus.publish("config.refresh.completed", refreshData);
        }
    }
    
    /**
     * 刷新单个字段值
     */
    private void refreshField(RefreshableField refreshableField, ConfigChangeEvent event) {
        try {
            Object bean = refreshableField.getBeanInstance();
            Field field = refreshableField.getField();
            String expression = refreshableField.getExpression();
            
            // 获取字段当前值
            field.setAccessible(true);
            Object oldValue = field.get(bean);
            
            // 解析新值
            Object newValue = resolveValue(expression, field.getType());
            
            // 比较值是否发生变化
            if (!Objects.equals(oldValue, newValue)) {
                // 设置新值
                field.set(bean, newValue);
                
                System.out.println(LOG_PREFIX + " 字段值已刷新: " + 
                                 refreshableField.getBeanName() + "." + field.getName() + 
                                 " [" + oldValue + " -> " + newValue + "]");
                
                // 发送字段刷新事件
                Map<String, Object> fieldData = new HashMap<>();
                fieldData.put("beanName", refreshableField.getBeanName());
                fieldData.put("fieldName", field.getName());
                fieldData.put("oldValue", oldValue);
                fieldData.put("newValue", newValue);
                fieldData.put("configKey", event.getFullConfigKey());
                LQEnhancedBus.publish("config.field.refreshed", fieldData);
            }
            
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " 字段刷新失败: " + 
                             refreshableField.getBeanName() + "." + 
                             refreshableField.getField().getName() + " - " + e.getMessage());
        }
    }
    
    /**
     * 解析配置值
     */
    private Object resolveValue(String expression, Class<?> targetType) {
        // 使用Spring的Environment解析占位符
        String resolvedValue = environment.resolvePlaceholders(expression);
        
        // 如果解析失败，尝试从配置中心获取
        if (resolvedValue.equals(expression)) {
            // 提取配置键
            Set<String> configKeys = extractConfigKeys(expression);
            for (String configKey : configKeys) {
                String configValue = LQEnhancedConfigCenter.getConfig(configKey);
                if (configValue != null) {
                    resolvedValue = expression.replace("${" + configKey + "}", configValue);
                    break;
                }
            }
        }
        
        // 类型转换
        return convertValue(resolvedValue, targetType);
    }
    
    /**
     * 值类型转换
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.valueOf(value);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.valueOf(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.valueOf(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.valueOf(value);
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.valueOf(value);
            } else {
                // 尝试JSON反序列化
                return LQUtil.jsonToBean(value, targetType);
            }
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " 值转换失败: " + value + " -> " + targetType.getSimpleName());
            return value; // 返回原始字符串值
        }
    }
    
    /**
     * 手动刷新指定Bean的所有@Value字段
     */
    public void refreshBean(String beanName) {
        Object bean = beanInstanceMap.get(beanName);
        if (bean == null) {
            System.err.println(LOG_PREFIX + " Bean不存在: " + beanName);
            return;
        }
        
        // 重新扫描并刷新
        scanValueAnnotations(bean, beanName);
        System.out.println(LOG_PREFIX + " Bean刷新完成: " + beanName);
    }
    
    /**
     * 手动刷新所有@Value字段
     */
    public void refreshAllBeans() {
        System.out.println(LOG_PREFIX + " 开始刷新所有Bean...");
        
        for (String beanName : beanInstanceMap.keySet()) {
            refreshBean(beanName);
        }
        
        System.out.println(LOG_PREFIX + " 所有Bean刷新完成");
    }
    
    /**
     * 获取配置刷新统计信息
     */
    public Map<String, Object> getRefreshStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("registeredBeans", beanInstanceMap.size());
        stats.put("registeredConfigs", configFieldMap.size());
        
        int totalFields = configFieldMap.values().stream()
                                       .mapToInt(List::size)
                                       .sum();
        stats.put("totalRefreshableFields", totalFields);
        
        Map<String, Integer> configFieldCounts = new HashMap<>();
        configFieldMap.forEach((key, fields) -> 
            configFieldCounts.put(key, fields.size())
        );
        stats.put("configFieldCounts", configFieldCounts);
        
        return stats;
    }
    
    /**
     * 可刷新字段信息
     */
    private static class RefreshableField {
        private final String beanName;
        private final Object beanInstance;
        private final Field field;
        private final String expression;
        private final String configKey;
        
        public RefreshableField(String beanName, Object beanInstance, Field field, 
                              String expression, String configKey) {
            this.beanName = beanName;
            this.beanInstance = beanInstance;
            this.field = field;
            this.expression = expression;
            this.configKey = configKey;
        }
        
        public String getBeanName() { return beanName; }
        public Object getBeanInstance() { return beanInstance; }
        public Field getField() { return field; }
        public String getExpression() { return expression; }
        public String getConfigKey() { return configKey; }
    }
}
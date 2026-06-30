package cn.lingque.cloud.config.enhanced;

import cn.hutool.json.JSONUtil;
import cn.lingque.util.LQUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * LQ配置管理器
 * 提供高级配置管理功能，包括配置模板、批量操作、配置验证等
 * 
 * @author LingQue Team
 */
@Slf4j
@Component
public class LQConfigManager {
    
    // 配置模板存储
    private static final Map<String, ConfigTemplate> CONFIG_TEMPLATES = new ConcurrentHashMap<>();
    
    // 配置验证器
    private static final Map<String, ConfigValidator> CONFIG_VALIDATORS = new ConcurrentHashMap<>();
    
    // 配置转换器
    private static final Map<String, ConfigConverter> CONFIG_CONVERTERS = new ConcurrentHashMap<>();
    
    /**
     * 注册配置模板
     * @param templateName 模板名称
     * @param template 配置模板
     */
    public static void registerTemplate(String templateName, ConfigTemplate template) {
        CONFIG_TEMPLATES.put(templateName, template);
        log.info("配置模板已注册: {}", templateName);
    }
    
    /**
     * 注册配置验证器
     * @param validatorName 验证器名称
     * @param validator 配置验证器
     */
    public static void registerValidator(String validatorName, ConfigValidator validator) {
        CONFIG_VALIDATORS.put(validatorName, validator);
        log.info("配置验证器已注册: {}", validatorName);
    }
    
    /**
     * 注册配置转换器
     * @param converterName 转换器名称
     * @param converter 配置转换器
     */
    public static void registerConverter(String converterName, ConfigConverter converter) {
        CONFIG_CONVERTERS.put(converterName, converter);
        log.info("配置转换器已注册: {}", converterName);
    }
    
    /**
     * 从模板创建配置
     * @param templateName 模板名称
     * @param namespace 命名空间
     * @param variables 模板变量
     */
    public static void createFromTemplate(String templateName, String namespace, Map<String, String> variables) {
        ConfigTemplate template = CONFIG_TEMPLATES.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("配置模板不存在: " + templateName);
        }
        
        Map<String, String> configs = template.generate(variables);
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            LQEnhancedConfigCenter.setConfig(namespace, entry.getKey(), entry.getValue());
        }
        
        log.info("从模板 {} 创建了 {} 个配置到命名空间 {}", templateName, configs.size(), namespace);
    }
    
    /**
     * 批量设置配置（带验证）
     * @param namespace 命名空间
     * @param configs 配置映射
     * @param validatorName 验证器名称
     */
    public static void setBatchConfigs(String namespace, Map<String, String> configs, String validatorName) {
        ConfigValidator validator = CONFIG_VALIDATORS.get(validatorName);
        
        // 验证配置
        if (validator != null) {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                if (!validator.validate(entry.getKey(), entry.getValue())) {
                    throw new IllegalArgumentException("配置验证失败: " + entry.getKey() + " = " + entry.getValue());
                }
            }
        }
        
        // 批量设置
        LQEnhancedConfigCenter.setConfigs(namespace, configs);
        log.info("批量设置配置完成: 命名空间={}, 数量={}, 验证器={}", namespace, configs.size(), validatorName);
    }
    
    /**
     * 导入YAML配置
     * @param namespace 命名空间
     * @param yamlContent YAML内容
     */
    @SuppressWarnings("unchecked")
    public static void importYamlConfig(String namespace, String yamlContent) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            
            Map<String, String> flatConfigs = flattenMap(yamlMap, "");
            LQEnhancedConfigCenter.setConfigs(namespace, flatConfigs);
            
            log.info("YAML配置导入完成: 命名空间={}, 配置数量={}", namespace, flatConfigs.size());
        } catch (Exception e) {
            log.error("YAML配置导入失败: 命名空间={}", namespace, e);
            throw new RuntimeException("YAML配置导入失败", e);
        }
    }
    
    /**
     * 导入JSON配置
     * @param namespace 命名空间
     * @param jsonContent JSON内容
     */
    @SuppressWarnings("unchecked")
    public static void importJsonConfig(String namespace, String jsonContent) {
        try {
            Map<String, Object> jsonMap = JSONUtil.toBean(jsonContent, Map.class);
            Map<String, String> flatConfigs = flattenMap(jsonMap, "");
            LQEnhancedConfigCenter.setConfigs(namespace, flatConfigs);
            
            log.info("JSON配置导入完成: 命名空间={}, 配置数量={}", namespace, flatConfigs.size());
        } catch (Exception e) {
            log.error("JSON配置导入失败: 命名空间={}", namespace, e);
            throw new RuntimeException("JSON配置导入失败", e);
        }
    }
    
    /**
     * 导入Properties配置
     * @param namespace 命名空间
     * @param propertiesContent Properties内容
     */
    public static void importPropertiesConfig(String namespace, String propertiesContent) {
        try {
            Properties properties = new Properties();
            properties.load(new java.io.StringReader(propertiesContent));
            
            Map<String, String> configs = new HashMap<>();
            for (String key : properties.stringPropertyNames()) {
                configs.put(key, properties.getProperty(key));
            }
            
            LQEnhancedConfigCenter.setConfigs(namespace, configs);
            log.info("Properties配置导入完成: 命名空间={}, 配置数量={}", namespace, configs.size());
        } catch (Exception e) {
            log.error("Properties配置导入失败: 命名空间={}", namespace, e);
            throw new RuntimeException("Properties配置导入失败", e);
        }
    }
    
    /**
     * 导出配置为YAML格式
     * @param namespace 命名空间
     * @return YAML字符串
     */
    public static String exportToYaml(String namespace) {
        Map<String, String> configs = LQEnhancedConfigCenter.getAllConfigs(namespace);
        Map<String, Object> nestedMap = unflattenMap(configs);
        
        Yaml yaml = new Yaml();
        return yaml.dump(nestedMap);
    }
    
    /**
     * 导出配置为JSON格式
     * @param namespace 命名空间
     * @return JSON字符串
     */
    public static String exportToJson(String namespace) {
        Map<String, String> configs = LQEnhancedConfigCenter.getAllConfigs(namespace);
        Map<String, Object> nestedMap = unflattenMap(configs);
        
        return JSONUtil.toJsonPrettyStr(nestedMap);
    }
    
    /**
     * 导出配置为Properties格式
     * @param namespace 命名空间
     * @return Properties字符串
     */
    public static String exportToProperties(String namespace) {
        Map<String, String> configs = LQEnhancedConfigCenter.getAllConfigs(namespace);
        
        StringBuilder sb = new StringBuilder();
        sb.append("# LQ配置中心导出 - 命名空间: ").append(namespace).append("\n");
        sb.append("# 导出时间: ").append(new Date()).append("\n\n");
        
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 搜索配置
     * @param namespace 命名空间
     * @param keyPattern 键模式（支持通配符）
     * @param valuePattern 值模式（支持正则表达式）
     * @return 匹配的配置
     */
    public static Map<String, String> searchConfigs(String namespace, String keyPattern, String valuePattern) {
        Map<String, String> allConfigs = LQEnhancedConfigCenter.getAllConfigs(namespace);
        Map<String, String> result = new HashMap<>();
        
        Pattern keyRegex = keyPattern != null ? Pattern.compile(keyPattern.replace("*", ".*")) : null;
        Pattern valueRegex = valuePattern != null ? Pattern.compile(valuePattern) : null;
        
        for (Map.Entry<String, String> entry : allConfigs.entrySet()) {
            boolean keyMatch = keyRegex == null || keyRegex.matcher(entry.getKey()).matches();
            boolean valueMatch = valueRegex == null || valueRegex.matcher(entry.getValue()).matches();
            
            if (keyMatch && valueMatch) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * 配置差异比较
     * @param namespace1 命名空间1
     * @param namespace2 命名空间2
     * @return 配置差异
     */
    public static ConfigDiff compareConfigs(String namespace1, String namespace2) {
        Map<String, String> configs1 = LQEnhancedConfigCenter.getAllConfigs(namespace1);
        Map<String, String> configs2 = LQEnhancedConfigCenter.getAllConfigs(namespace2);
        
        ConfigDiff diff = new ConfigDiff();
        
        // 找出只在namespace1中存在的配置
        for (Map.Entry<String, String> entry : configs1.entrySet()) {
            if (!configs2.containsKey(entry.getKey())) {
                diff.getOnlyInFirst().put(entry.getKey(), entry.getValue());
            } else if (!Objects.equals(entry.getValue(), configs2.get(entry.getKey()))) {
                diff.getDifferent().put(entry.getKey(), 
                    new String[]{entry.getValue(), configs2.get(entry.getKey())});
            } else {
                diff.getSame().put(entry.getKey(), entry.getValue());
            }
        }
        
        // 找出只在namespace2中存在的配置
        for (Map.Entry<String, String> entry : configs2.entrySet()) {
            if (!configs1.containsKey(entry.getKey())) {
                diff.getOnlyInSecond().put(entry.getKey(), entry.getValue());
            }
        }
        
        return diff;
    }
    
    /**
     * 扁平化Map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> flattenMap(Map<String, Object> map, String prefix) {
        Map<String, String> result = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                result.putAll(flattenMap((Map<String, Object>) value, key));
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        result.putAll(flattenMap((Map<String, Object>) item, key + "[" + i + "]"));
                    } else {
                        result.put(key + "[" + i + "]", String.valueOf(item));
                    }
                }
            } else {
                result.put(key, String.valueOf(value));
            }
        }
        
        return result;
    }
    
    /**
     * 反扁平化Map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unflattenMap(Map<String, String> flatMap) {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, String> entry : flatMap.entrySet()) {
            String[] keys = entry.getKey().split("\\.");
            Map<String, Object> current = result;
            
            for (int i = 0; i < keys.length - 1; i++) {
                String key = keys[i];
                if (!current.containsKey(key)) {
                    current.put(key, new HashMap<String, Object>());
                }
                current = (Map<String, Object>) current.get(key);
            }
            
            current.put(keys[keys.length - 1], entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 配置模板接口
     */
    public interface ConfigTemplate {
        Map<String, String> generate(Map<String, String> variables);
    }
    
    /**
     * 配置验证器接口
     */
    public interface ConfigValidator {
        boolean validate(String key, String value);
    }
    
    /**
     * 配置转换器接口
     */
    public interface ConfigConverter {
        String convert(String value);
    }
    
    /**
     * 配置差异类
     */
    public static class ConfigDiff {
        private Map<String, String> onlyInFirst = new HashMap<>();
        private Map<String, String> onlyInSecond = new HashMap<>();
        private Map<String, String[]> different = new HashMap<>();
        private Map<String, String> same = new HashMap<>();
        
        // Getters
        public Map<String, String> getOnlyInFirst() { return onlyInFirst; }
        public Map<String, String> getOnlyInSecond() { return onlyInSecond; }
        public Map<String, String[]> getDifferent() { return different; }
        public Map<String, String> getSame() { return same; }
        
        @Override
        public String toString() {
            return String.format("ConfigDiff{onlyInFirst=%d, onlyInSecond=%d, different=%d, same=%d}",
                    onlyInFirst.size(), onlyInSecond.size(), different.size(), same.size());
        }
    }
}
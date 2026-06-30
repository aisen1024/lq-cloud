package com.paypay.lqadmin.service;

import cn.lingque.cloud.config.enhanced.LQEnhancedConfigCenter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置中心服务
 */
@Service
public class ConfigCenterService {

    // 用于跟踪已知的命名空间（租户）
    private final Set<String> knownNamespaces = ConcurrentHashMap.newKeySet();
    
    // 命名空间显示名称映射
    private final Map<String, String> namespaceShowNames = new ConcurrentHashMap<>();
    
    public ConfigCenterService() {
        // 初始化默认命名空间
        knownNamespaces.add("public");  // 公共命名空间，类似 Nacos 的 public
        namespaceShowNames.put("public", "公共空间");
    }

    /**
     * 获取所有命名空间
     */
    public List<String> getAllNamespaces() {
        return new ArrayList<>(knownNamespaces);
    }

    /**
     * 注册命名空间（当设置配置时调用）
     */
    public void registerNamespace(String namespace) {
        if (namespace != null && !namespace.isEmpty()) {
            knownNamespaces.add(namespace);
            // 如果没有显示名称，使用命名空间ID作为显示名称
            namespaceShowNames.putIfAbsent(namespace, namespace);
        }
    }

    /**
     * 创建命名空间
     */
    public void createNamespace(String namespace, String namespaceShowName) {
        knownNamespaces.add(namespace);
        namespaceShowNames.put(namespace, namespaceShowName);
    }

    /**
     * 删除命名空间
     */
    public void deleteNamespace(String namespace) {
        if (!namespace.equals("public")) {
            knownNamespaces.remove(namespace);
            namespaceShowNames.remove(namespace);
        }
    }

    /**
     * 获取命名空间显示名称
     */
    public String getNamespaceShowName(String namespace) {
        return namespaceShowNames.getOrDefault(namespace, namespace);
    }

    /**
     * 获取命名空间的配置数量
     */
    public int getConfigCount(String namespace) {
        Map<String, String> configs = LQEnhancedConfigCenter.getAllConfigs(namespace);
        return configs.size();
    }

    /**
     * 检查命名空间是否存在配置
     */
    public boolean hasConfigs(String namespace) {
        return getConfigCount(namespace) > 0;
    }
    
    /**
     * 创建示例配置
     */
    public void createExampleConfig(String appName, String profile, String tenant) {
        String dataId = appName + "-" + profile + ".yml";
        String content = generateExampleYaml(appName, profile);
        LQEnhancedConfigCenter.setConfig(tenant, dataId, content);
        registerNamespace(tenant);
    }
    
    /**
     * 生成示例 YAML 配置
     */
    private String generateExampleYaml(String appName, String profile) {
        return String.format(
            "# %s 配置文件 (%s 环境)\n" +
            "server:\n" +
            "  port: 8080\n" +
            "  servlet:\n" +
            "    context-path: /%s\n" +
            "\n" +
            "spring:\n" +
            "  application:\n" +
            "    name: %s\n" +
            "  profiles:\n" +
            "    active: %s\n" +
            "\n" +
            "# 自定义配置\n" +
            "app:\n" +
            "  name: %s\n" +
            "  version: 1.0.0\n",
            appName, profile, appName, appName, profile, appName
        );
    }
}

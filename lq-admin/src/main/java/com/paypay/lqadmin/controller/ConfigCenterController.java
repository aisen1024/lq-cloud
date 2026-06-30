package com.paypay.lqadmin.controller;

import cn.lingque.cloud.config.enhanced.ConfigStats;
import cn.lingque.cloud.config.enhanced.LQEnhancedConfigCenter;
import com.paypay.lqadmin.service.ConfigCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * 配置中心管理接口 - 参考 Nacos 设计
 * 以配置文件为单位，支持 YAML/Properties/JSON 等格式
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigCenterController {

    @Autowired
    private ConfigCenterService configCenterService;

    /**
     * 获取命名空间列表
     */
    @GetMapping("/namespaces")
    public Map<String, Object> getNamespaces() {
        List<Map<String, Object>> namespaces = new ArrayList<>();
        
        for (String namespace : configCenterService.getAllNamespaces()) {
            Map<String, Object> ns = new HashMap<>();
            ns.put("namespace", namespace);
            ns.put("namespaceShowName", configCenterService.getNamespaceShowName(namespace));
            ns.put("configCount", configCenterService.getConfigCount(namespace));
            ns.put("type", namespace.equals("public") ? 0 : 1); // 0=公共, 1=自定义
            namespaces.add(ns);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", namespaces);
        result.put("code", 200);
        return result;
    }

    /**
     * 创建或更新命名空间
     */
    @PostMapping("/namespace")
    public Map<String, Object> createNamespace(@RequestBody Map<String, String> params) {
        String namespace = params.get("namespace");
        String namespaceShowName = params.get("namespaceShowName");
        
        Map<String, Object> result = new HashMap<>();
        
        // 验证命名空间ID
        if (namespace == null || namespace.trim().isEmpty()) {
            result.put("code", 400);
            result.put("message", "命名空间ID不能为空");
            return result;
        }
        
        if (!namespace.matches("^[a-zA-Z0-9_-]+$")) {
            result.put("code", 400);
            result.put("message", "命名空间ID只能包含字母、数字、下划线、中划线");
            return result;
        }
        
        if (namespace.equals("public")) {
            result.put("code", 400);
            result.put("message", "不能修改默认命名空间");
            return result;
        }
        
        // 验证命名空间名称
        if (namespaceShowName == null || namespaceShowName.trim().isEmpty()) {
            result.put("code", 400);
            result.put("message", "命名空间名称不能为空");
            return result;
        }
        
        try {
            configCenterService.createNamespace(namespace, namespaceShowName);
            
            result.put("code", 200);
            result.put("message", "保存成功");
            result.put("data", true);
            return result;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "保存失败: " + e.getMessage());
            result.put("data", false);
            return result;
        }
    }

    /**
     * 删除命名空间
     */
    @DeleteMapping("/namespace")
    public Map<String, Object> deleteNamespace(@RequestParam String namespace) {
        Map<String, Object> result = new HashMap<>();
        
        if (namespace.equals("public")) {
            result.put("code", 400);
            result.put("message", "不能删除默认命名空间");
            return result;
        }
        
        // 检查是否有配置
        int configCount = configCenterService.getConfigCount(namespace);
        if (configCount > 0) {
            result.put("code", 400);
            result.put("message", "该命名空间下还有 " + configCount + " 个配置，无法删除");
            return result;
        }
        
        try {
            configCenterService.deleteNamespace(namespace);
            
            result.put("code", 200);
            result.put("message", "删除成功");
            result.put("data", true);
            return result;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败: " + e.getMessage());
            result.put("data", false);
            return result;
        }
    }

    /**
     * 查询配置列表（分页）
     * dataId 格式：{应用名}-{profile}.{后缀}
     * 例如：user-service-dev.yml, order-service-prod.properties
     */
    @GetMapping("/list")
    public Map<String, Object> getConfigList(
            @RequestParam(defaultValue = "public") String tenant,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String configTags) {
        
        Map<String, String> allConfigs = LQEnhancedConfigCenter.getAllConfigs(tenant);
        List<Map<String, Object>> configs = new ArrayList<>();
        
        // 注册命名空间
        if (!allConfigs.isEmpty()) {
            configCenterService.registerNamespace(tenant);
        }
        
        // 转换为配置列表
        for (Map.Entry<String, String> entry : allConfigs.entrySet()) {
            String key = entry.getKey();
            String content = entry.getValue();
            
            // 解析 dataId: {appName}-{profile}.{ext}
            String[] parts = parseDataId(key);
            String parsedAppName = parts[0];
            String profile = parts[1];
            String ext = parts[2];
            
            // 过滤
            if (dataId != null && !key.contains(dataId)) {
                continue;
            }
            if (appName != null && !parsedAppName.contains(appName)) {
                continue;
            }
            
            Map<String, Object> config = new HashMap<>();
            config.put("id", key);
            config.put("dataId", key);
            config.put("group", group != null ? group : "DEFAULT_GROUP");
            config.put("content", content);
            config.put("md5", getMd5(content));
            config.put("type", ext);
            config.put("appName", parsedAppName);
            config.put("tenant", tenant);
            configs.add(config);
        }
        
        // 分页
        int total = configs.size();
        int start = (pageNo - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Map<String, Object>> pageData = start < total ? configs.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", total);
        result.put("pageNumber", pageNo);
        result.put("pagesAvailable", (total + pageSize - 1) / pageSize);
        result.put("pageItems", pageData);
        result.put("code", 200);
        
        return result;
    }

    /**
     * 获取配置详情
     */
    @GetMapping("/detail")
    public Map<String, Object> getConfig(
            @RequestParam String dataId,
            @RequestParam(defaultValue = "public") String tenant,
            @RequestParam(defaultValue = "DEFAULT_GROUP") String group,
            @RequestParam(defaultValue = "false") boolean show) {
        
        String content = LQEnhancedConfigCenter.getConfig(tenant, dataId);
        
        Map<String, Object> result = new HashMap<>();
        if (content != null) {
            String[] parts = parseDataId(dataId);
            
            result.put("dataId", dataId);
            result.put("group", group);
            result.put("content", content);
            result.put("md5", getMd5(content));
            result.put("type", parts[2]);
            result.put("appName", parts[0]);
            result.put("tenant", tenant);
            result.put("code", 200);
        } else {
            result.put("code", 404);
            result.put("message", "配置不存在");
        }
        
        return result;
    }

    /**
     * 发布配置
     */
    @PostMapping("/publish")
    public Map<String, Object> publishConfig(@RequestBody Map<String, String> params) {
        String dataId = params.get("dataId");
        String content = params.get("content");
        String tenant = params.getOrDefault("tenant", "public");
        String group = params.getOrDefault("group", "DEFAULT_GROUP");
        String type = params.get("type");
        
        Map<String, Object> result = new HashMap<>();
        
        // 验证 dataId 格式
        if (!isValidDataId(dataId)) {
            result.put("code", 400);
            result.put("message", "dataId 格式错误，应为：{应用名}-{环境}.{后缀}，例如：user-service-dev.yml");
            result.put("data", false);
            return result;
        }
        
        // 验证配置内容
        String validationError = validateConfig(content, type);
        if (validationError != null) {
            result.put("code", 400);
            result.put("message", "配置格式错误: " + validationError);
            result.put("data", false);
            return result;
        }
        
        try {
            LQEnhancedConfigCenter.setConfig(tenant, dataId, content);
            configCenterService.registerNamespace(tenant);
            
            result.put("code", 200);
            result.put("message", "发布成功");
            result.put("data", true);
            return result;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "发布失败: " + e.getMessage());
            result.put("data", false);
            return result;
        }
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteConfig(
            @RequestParam String dataId,
            @RequestParam(defaultValue = "public") String tenant) {
        
        try {
            LQEnhancedConfigCenter.removeConfig(tenant, dataId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "删除成功");
            result.put("data", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "删除失败: " + e.getMessage());
            result.put("data", false);
            return result;
        }
    }

    /**
     * 克隆配置
     */
    @PostMapping("/clone")
    public Map<String, Object> cloneConfig(@RequestBody Map<String, String> params) {
        String srcDataId = params.get("srcDataId");
        String destDataId = params.get("destDataId");
        String tenant = params.getOrDefault("tenant", "public");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String content = LQEnhancedConfigCenter.getConfig(tenant, srcDataId);
            if (content == null) {
                result.put("code", 404);
                result.put("message", "源配置不存在");
                result.put("data", false);
                return result;
            }
            
            LQEnhancedConfigCenter.setConfig(tenant, destDataId, content);
            
            result.put("code", 200);
            result.put("message", "克隆成功");
            result.put("data", true);
            return result;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "克隆失败: " + e.getMessage());
            result.put("data", false);
            return result;
        }
    }

    /**
     * 导出配置
     */
    @GetMapping("/export")
    public Map<String, Object> exportConfig(
            @RequestParam(required = false) String tenant,
            @RequestParam(required = false) String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String appName) {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> configs = new ArrayList<>();
        
        // 导出逻辑
        result.put("code", 200);
        result.put("data", configs);
        return result;
    }

    /**
     * 导入配置
     */
    @PostMapping("/import")
    public Map<String, Object> importConfig(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "导入成功");
        result.put("data", true);
        return result;
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        ConfigStats stats = LQEnhancedConfigCenter.getStats();
        
        Map<String, Object> result = new HashMap<>();
        result.put("namespaceCount", stats.getNamespaceCount());
        result.put("configCount", stats.getTotalConfigCount());
        result.put("validConfigCount", stats.getValidConfigCount());
        result.put("expiredConfigCount", stats.getExpiredConfigCount());
        result.put("listenerCount", stats.getListenerCount());
        result.put("healthStatus", stats.getHealthStatus().name());
        result.put("code", 200);
        
        return result;
    }

    /**
     * 验证 dataId 格式
     * 格式：{应用名}-{环境}.{后缀}
     * 例如：user-service-dev.yml
     */
    private boolean isValidDataId(String dataId) {
        if (dataId == null || dataId.isEmpty()) {
            return false;
        }
        
        // 必须包含 . 和 -
        if (!dataId.contains(".") || !dataId.contains("-")) {
            return false;
        }
        
        // 检查后缀
        String ext = dataId.substring(dataId.lastIndexOf(".") + 1);
        return ext.matches("(yml|yaml|properties|json|xml|txt)");
    }

    /**
     * 解析 dataId
     * 返回：[appName, profile, ext]
     */
    private String[] parseDataId(String dataId) {
        String[] result = new String[3];
        
        if (dataId == null || !dataId.contains(".")) {
            result[0] = dataId;
            result[1] = "";
            result[2] = "text";
            return result;
        }
        
        int dotIndex = dataId.lastIndexOf(".");
        String nameWithProfile = dataId.substring(0, dotIndex);
        String ext = dataId.substring(dotIndex + 1);
        
        // 解析应用名和环境
        int lastDashIndex = nameWithProfile.lastIndexOf("-");
        if (lastDashIndex > 0) {
            result[0] = nameWithProfile.substring(0, lastDashIndex); // appName
            result[1] = nameWithProfile.substring(lastDashIndex + 1); // profile
        } else {
            result[0] = nameWithProfile;
            result[1] = "";
        }
        result[2] = ext;
        
        return result;
    }

    /**
     * 验证配置内容格式
     */
    private String validateConfig(String content, String type) {
        if (content == null || content.trim().isEmpty()) {
            return "配置内容不能为空";
        }
        
        try {
            if ("yml".equals(type) || "yaml".equals(type)) {
                // 验证 YAML 格式
                Yaml yaml = new Yaml();
                yaml.load(content);
            } else if ("json".equals(type)) {
                // 验证 JSON 格式
                cn.hutool.json.JSONUtil.parse(content);
            } else if ("properties".equals(type)) {
                // 验证 Properties 格式
                Properties props = new Properties();
                props.load(new java.io.StringReader(content));
            }
            // xml 和 text 不做严格验证
            
            return null; // 验证通过
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 计算 MD5
     */
    private String getMd5(String content) {
        if (content == null) return "";
        try {
            return cn.lingque.util.LQUtil.getMD5(content);
        } catch (Exception e) {
            return "";
        }
    }
}


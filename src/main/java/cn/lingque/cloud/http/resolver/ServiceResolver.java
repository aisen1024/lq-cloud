package cn.lingque.cloud.http.resolver;

import cn.hutool.core.util.StrUtil;
import cn.lingque.cloud.node.LQRegisterCenter;
import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.cloud.loadbalance.LqBalanceService;
import cn.lingque.runner.config.LqSpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * 服务解析器
 * 负责服务发现和负载均衡
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class ServiceResolver {
    
    /**
     * 解析服务URL
     */
    public String resolveServiceUrl(String serviceName) {
        if (StrUtil.isBlank(serviceName)) {
            throw new IllegalArgumentException("服务名称不能为空");
        }
        
        // 检查是否为HTTP域名地址
        if (isHttpUrl(serviceName)) {
            return handleHttpUrls(serviceName);
        }
        
        try {
            // 尝试使用现有的负载均衡服务
            LqBalanceService balanceService = LqSpringUtil.getBean(LqBalanceService.class);
            if (balanceService != null) {
                LQNodeInfo nodeInfo = balanceService.pollingBalance(serviceName);
                if (nodeInfo != null) {
                    return nodeInfo.getUri("http").toString();
                }
            }
        } catch (Exception e) {
            log.warn("使用负载均衡服务失败，尝试直接从注册中心获取: {}", e.getMessage());
        }
        
        try {
            // 直接从注册中心获取服务节点
            Set<LQNodeInfo> nodes = LQRegisterCenter.getSvNodeList(serviceName);
            if (nodes != null && !nodes.isEmpty()) {
                // 简单的轮询负载均衡
                LQNodeInfo nodeInfo = nodes.iterator().next();
                return nodeInfo.getUri("http").toString();
            }
        } catch (Exception e) {
            log.error("从注册中心获取服务节点失败: {}", serviceName, e);
        }
        
        throw new RuntimeException("无法解析服务: " + serviceName);
    }
    
    /**
     * 检查是否为HTTP URL
     */
    private boolean isHttpUrl(String serviceName) {
        return serviceName.startsWith("http://") || serviceName.startsWith("https://") || 
               serviceName.contains(".") && !serviceName.contains(" ");
    }
    
    /**
     * 处理HTTP URL地址（支持多个地址轮询）
     */
    private String handleHttpUrls(String serviceName) {
        // 支持多个域名用逗号分隔
        String[] urls = serviceName.split(",");
        
        if (urls.length == 1) {
            // 单个URL直接返回
            String url = urls[0].trim();
            return ensureHttpPrefix(url);
        } else {
            // 多个URL进行轮询
            return selectUrlByRoundRobin(urls);
        }
    }
    
    /**
     * 确保URL有HTTP前缀
     */
    private String ensureHttpPrefix(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://" + url;
        }
        return url;
    }
    
    /**
     * 轮询选择URL
     */
    private String selectUrlByRoundRobin(String[] urls) {
        // 使用简单的轮询算法
        long currentTime = System.currentTimeMillis();
        int index = (int) (currentTime % urls.length);
        
        String selectedUrl = urls[index].trim();
        log.debug("轮询选择URL: {} (索引: {})", selectedUrl, index);
        
        return ensureHttpPrefix(selectedUrl);
    }
}
package cn.lingque.cloud.mcp.example;

import cn.lingque.cloud.mcp.handler.LQMCPToolHandler;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LQ MCP工具使用示例
 * 演示如何使用MCP工具进行各种操作
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
public class LQMCPToolUsageExample implements CommandLineRunner {

    @Autowired
    private LQMCPToolHandler mcpToolHandler;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== LQ MCP工具使用示例开始 ===");
        
        // 等待一下确保工具注册完成
        Thread.sleep(2000);
        
        // 示例1: 创建测试文件
        demonstrateFileCreation();
        
        // 示例2: 读取文件内容
        demonstrateFileReading();
        
        // 示例3: 列出目录内容
        demonstrateDirectoryListing();
        
        // 示例4: 获取文件信息
        demonstrateFileInfo();
        
        // 示例5: 搜索文件
        demonstrateFileSearch();
        
        // 示例6: 异步调用
        demonstrateAsyncCall();
        
        // 示例7: 批量调用
        demonstrateBatchCall();
        
        log.info("=== LQ MCP工具使用示例结束 ===");
    }

    /**
     * 演示文件创建
     */
    private void demonstrateFileCreation() {
        log.info("\n--- 示例1: 创建测试文件 ---");
        
        try {
            String testContent = "这是一个MCP工具测试文件\n创建时间: " + System.currentTimeMillis();
            
            LQMCPToolManager.MCPToolResponse response = mcpToolHandler.callTool(
                    "file-manager", 
                    "writeFile", 
                    Map.of(
                            "filePath", "/tmp/mcp-test.txt",
                            "content", testContent,
                            "append", false
                    )
            );
            
            if (response.isSuccess()) {
                log.info("✅ 文件创建成功: {}", response.getMessage());
                log.info("📄 响应数据: {}", response.getData());
            } else {
                log.error("❌ 文件创建失败: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ 文件创建异常", e);
        }
    }

    /**
     * 演示文件读取
     */
    private void demonstrateFileReading() {
        log.info("\n--- 示例2: 读取文件内容 ---");
        
        try {
            LQMCPToolManager.MCPToolResponse response = mcpToolHandler.callTool(
                    "file-manager", 
                    "readFile", 
                    Map.of("filePath", "/tmp/mcp-test.txt")
            );
            
            if (response.isSuccess()) {
                log.info("✅ 文件读取成功: {}", response.getMessage());
                log.info("📄 响应数据: {}", response.getData());
            } else {
                log.error("❌ 文件读取失败: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ 文件读取异常", e);
        }
    }

    /**
     * 演示目录列表
     */
    private void demonstrateDirectoryListing() {
        log.info("\n--- 示例3: 列出目录内容 ---");
        
        try {
            LQMCPToolManager.MCPToolResponse response = mcpToolHandler.callTool(
                    "file-manager", 
                    "listDirectory", 
                    Map.of(
                            "dirPath", "/tmp",
                            "includeHidden", false
                    )
            );
            
            if (response.isSuccess()) {
                log.info("✅ 目录列表获取成功: {}", response.getMessage());
                log.info("📁 响应数据: {}", response.getData());
            } else {
                log.error("❌ 目录列表获取失败: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ 目录列表获取异常", e);
        }
    }

    /**
     * 演示文件信息获取
     */
    private void demonstrateFileInfo() {
        log.info("\n--- 示例4: 获取文件信息 ---");
        
        try {
            LQMCPToolManager.MCPToolResponse response = mcpToolHandler.callTool(
                    "file-manager", 
                    "getFileInfo", 
                    Map.of("filePath", "/tmp/mcp-test.txt")
            );
            
            if (response.isSuccess()) {
                log.info("✅ 文件信息获取成功: {}", response.getMessage());
                log.info("ℹ️ 响应数据: {}", response.getData());
            } else {
                log.error("❌ 文件信息获取失败: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ 文件信息获取异常", e);
        }
    }

    /**
     * 演示文件搜索
     */
    private void demonstrateFileSearch() {
        log.info("\n--- 示例5: 搜索文件 ---");
        
        try {
            LQMCPToolManager.MCPToolResponse response = mcpToolHandler.callTool(
                    "file-manager", 
                    "searchFiles", 
                    Map.of(
                            "dirPath", "/tmp",
                            "pattern", "mcp",
                            "recursive", false
                    )
            );
            
            if (response.isSuccess()) {
                log.info("✅ 文件搜索成功: {}", response.getMessage());
                log.info("🔍 响应数据: {}", response.getData());
            } else {
                log.error("❌ 文件搜索失败: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ 文件搜索异常", e);
        }
    }

    /**
     * 演示异步调用
     */
    private void demonstrateAsyncCall() {
        log.info("\n--- 示例6: 异步调用 ---");
        
        try {
            CompletableFuture<LQMCPToolManager.MCPToolResponse> future = mcpToolHandler.callToolAsync(
                    "file-manager", 
                    "getFileInfo",
                    Map.of("filePath", "/tmp")
            );
            
            log.info("🚀 异步调用已启动，等待结果...");
            
            LQMCPToolManager.MCPToolResponse response = future.get();
            
            if (response.isSuccess()) {
                log.info("✅ 异步调用成功: {}", response.getMessage());
                log.info("⚡ 响应数据: {}", response.getData());
            } else {
                log.error("❌ 异步调用失败: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ 异步调用异常", e);
        }
    }

    /**
     * 演示批量调用
     */
    private void demonstrateBatchCall() {
        log.info("\n--- 示例7: 批量调用 ---");
        
        try {
            Map<String, Map<String, Object>> batchCalls = Map.of(
                    "call1", Map.of(
                            "toolName", "file-manager",
                            "methodName", "getFileInfo",
                            "parameters", Map.of("filePath", "/tmp")
                    ),
                    "call2", Map.of(
                            "toolName", "file-manager",
                            "methodName", "listDirectory",
                            "parameters", Map.of(
                                    "dirPath", "/tmp",
                                    "includeHidden", false
                            )
                    ),
                    "call3", Map.of(
                            "toolName", "file-manager",
                            "methodName", "searchFiles",
                            "parameters", Map.of(
                                    "dirPath", "/tmp",
                                    "pattern", "test",
                                    "recursive", false
                            )
                    )
            );
            
            CompletableFuture<Map<String, LQMCPToolManager.MCPToolResponse>> future = 
                    mcpToolHandler.callToolsBatchAsync(batchCalls);
            
            log.info("🚀 批量调用已启动，等待结果...");
            
            Map<String, LQMCPToolManager.MCPToolResponse> results = future.get();
            
            log.info("✅ 批量调用完成，结果数量: {}", results.size());
            
            for (Map.Entry<String, LQMCPToolManager.MCPToolResponse> entry : results.entrySet()) {
                String callId = entry.getKey();
                LQMCPToolManager.MCPToolResponse response = entry.getValue();
                
                if (response.isSuccess()) {
                    log.info("✅ 批量调用 {} 成功: {}", callId, response.getMessage());
                } else {
                    log.error("❌ 批量调用 {} 失败: {}", callId, response.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 批量调用异常", e);
        }
    }
}
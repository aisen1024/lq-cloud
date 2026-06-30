package cn.lingque.cloud.langchain;

import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LangChainExample {

    public static void main(String[] args) {
        // 1. 发现工具
        LQMCPToolManager.MCPToolDefinition tool = LQMCPToolManager.discoverTools("langchain-mcp-tool").get(0);

        // 2. 构造请求
        LQMCPToolManager.MCPToolRequest request = new LQMCPToolManager.MCPToolRequest();
        request.getParameters().put("question", "hello");

        // 3. 调用工具
        try {
            LQMCPToolManager.MCPToolResponse response = LQMCPToolManager.invokeTool(
                    tool.getToolName(), tool.getToolVersion(), request);
            log.info("MCP工具调用结果: 成功={}, 响应={}", response.isSuccess(), response.getData());
        } catch (Exception e) {
            log.error("MCP工具调用失败", e);
        }
    }
}
package cn.lingque.cloud.langchain;

import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class LangChainMCPTool {

    @Autowired
    private LQEnhancedRegisterCenter registerCenter;

    @PostConstruct
    public void init() {
        LQMCPToolManager.MCPToolDefinition toolDef = new LQMCPToolManager.MCPToolDefinition()
                .setToolName("langchain-mcp-tool")
                .setToolVersion("1.0");
        LQEnhancedNodeInfo nodeInfo = new LQEnhancedNodeInfo()
                .setServerName("langchain-mcp-tool")
                .setNodeIp("127.0.0.1")
                .setNodePort(8080)
                .setProtocol("MCP");
        LQMCPToolManager.registerMCPTool(toolDef, nodeInfo);
    }

    public String ask(String question) {
        ChatLanguageModel model = OpenAiChatModel.builder().apiKey("demo").build();
        Assistant assistant = AiServices.create(Assistant.class, model);
        return assistant.chat(question);
    }

    interface Assistant {
        String chat(String message);
    }
}
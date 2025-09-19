# LangChain4j Integration

This document describes the integration of LangChain4j into the lq-cloud project.

## MCP Tool

A new MCP tool has been created that uses LangChain4j to interact with the OpenAI API. The tool is defined in the `cn.lingque.cloud.langchain.LangChainMCPTool` class.

### Registration

The tool is automatically registered as an MCP tool when the application starts. The tool's name is `langchain-mcp-tool` and its version is `1.0`.

### Usage

The tool can be invoked using the `LQMCPToolManager`. The tool takes a single parameter, `question`, which is the question to ask the AI.

## Example

An example of how to use the tool is provided in the `cn.lingque.cloud.langchain.LangChainExample` class.

To run the example, you need to have a running instance of the lq-cloud application.
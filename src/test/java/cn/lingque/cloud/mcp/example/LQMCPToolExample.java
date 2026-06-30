package cn.lingque.cloud.mcp.example;

import cn.lingque.cloud.mcp.annotation.MCPTool;
import cn.lingque.cloud.mcp.annotation.MCPToolMethod;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LQ MCP工具示例
 * 演示如何创建和使用MCP工具
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
@MCPTool(name = "file-manager", version = "1.0.0", description = "文件管理工具，提供文件操作功能")
public class LQMCPToolExample {

    /**
     * 读取文件内容
     */
    @MCPToolMethod(name = "readFile", description = "读取指定文件的内容")
    public LQMCPToolManager.MCPToolResponse readFile(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return LQMCPToolManager.MCPToolResponse.error("文件路径不能为空");
            }
            
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return LQMCPToolManager.MCPToolResponse.error("文件不存在: " + filePath);
            }
            
            if (!Files.isRegularFile(path)) {
                return LQMCPToolManager.MCPToolResponse.error("不是有效的文件: " + filePath);
            }
            
            String content = Files.readString(path);
            
            Map<String, Object> result = Map.of(
                    "filePath", filePath,
                    "content", content,
                    "size", content.length(),
                    "readTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            log.info("[MCP-FileManager] 成功读取文件: {}, 大小: {} 字符", filePath, content.length());
            return LQMCPToolManager.MCPToolResponse.success("文件读取成功", result);
            
        } catch (IOException e) {
            log.error("[MCP-FileManager] 读取文件失败: {}", filePath, e);
            return LQMCPToolManager.MCPToolResponse.error("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 写入文件内容
     */
    @MCPToolMethod(name = "writeFile", description = "向指定文件写入内容")
    public LQMCPToolManager.MCPToolResponse writeFile(String filePath, String content, Boolean append) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return LQMCPToolManager.MCPToolResponse.error("文件路径不能为空");
            }
            
            if (content == null) {
                content = "";
            }
            
            Path path = Paths.get(filePath);
            
            // 确保父目录存在
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 写入文件
            if (Boolean.TRUE.equals(append) && Files.exists(path)) {
                Files.writeString(path, content, java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content);
            }
            
            Map<String, Object> result = Map.of(
                    "filePath", filePath,
                    "contentLength", content.length(),
                    "append", Boolean.TRUE.equals(append),
                    "writeTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            log.info("[MCP-FileManager] 成功写入文件: {}, 内容长度: {} 字符, 追加模式: {}", 
                    filePath, content.length(), Boolean.TRUE.equals(append));
            return LQMCPToolManager.MCPToolResponse.success("文件写入成功", result);
            
        } catch (IOException e) {
            log.error("[MCP-FileManager] 写入文件失败: {}", filePath, e);
            return LQMCPToolManager.MCPToolResponse.error("写入文件失败: " + e.getMessage());
        }
    }

    /**
     * 列出目录内容
     */
    @MCPToolMethod(name = "listDirectory", description = "列出指定目录的内容")
    public LQMCPToolManager.MCPToolResponse listDirectory(String dirPath, Boolean includeHidden) {
        try {
            if (dirPath == null || dirPath.trim().isEmpty()) {
                return LQMCPToolManager.MCPToolResponse.error("目录路径不能为空");
            }
            
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                return LQMCPToolManager.MCPToolResponse.error("目录不存在: " + dirPath);
            }
            
            if (!Files.isDirectory(path)) {
                return LQMCPToolManager.MCPToolResponse.error("不是有效的目录: " + dirPath);
            }
            
            List<Map<String, Object>> fileList = Files.list(path)
                    .filter(p -> Boolean.TRUE.equals(includeHidden) || !p.getFileName().toString().startsWith("."))
                    .map(p -> {
                        try {
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("name", p.getFileName().toString());
                            fileInfo.put("path", p.toString());
                            fileInfo.put("isDirectory", Files.isDirectory(p));
                            fileInfo.put("isFile", Files.isRegularFile(p));
                            fileInfo.put("size", Files.isRegularFile(p) ? Files.size(p) : 0);
                            fileInfo.put("lastModified", Files.getLastModifiedTime(p).toString());
                            return (Map<String, Object>) fileInfo;
                        } catch (IOException e) {
                            log.warn("[MCP-FileManager] 获取文件信息失败: {}", p, e);
                            Map<String, Object> errorInfo = new HashMap<>();
                            errorInfo.put("name", p.getFileName().toString());
                            errorInfo.put("error", e.getMessage());
                            return errorInfo;
                        }
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> result = Map.of(
                    "directory", dirPath,
                    "files", fileList,
                    "count", fileList.size(),
                    "includeHidden", Boolean.TRUE.equals(includeHidden),
                    "listTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            log.info("[MCP-FileManager] 成功列出目录: {}, 文件数量: {}", dirPath, fileList.size());
            return LQMCPToolManager.MCPToolResponse.success("目录列表获取成功", result);
            
        } catch (IOException e) {
            log.error("[MCP-FileManager] 列出目录失败: {}", dirPath, e);
            return LQMCPToolManager.MCPToolResponse.error("列出目录失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件或目录
     */
    @MCPToolMethod(name = "deleteFile", description = "删除指定的文件或目录")
    public LQMCPToolManager.MCPToolResponse deleteFile(String filePath, Boolean recursive) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return LQMCPToolManager.MCPToolResponse.error("文件路径不能为空");
            }
            
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return LQMCPToolManager.MCPToolResponse.error("文件或目录不存在: " + filePath);
            }
            
            boolean isDirectory = Files.isDirectory(path);
            long size = 0;
            
            if (isDirectory) {
                if (Boolean.TRUE.equals(recursive)) {
                    // 递归删除目录
                    size = Files.walk(path)
                            .sorted(Comparator.reverseOrder())
                            .mapToLong(p -> {
                                try {
                                    long fileSize = Files.isRegularFile(p) ? Files.size(p) : 0;
                                    Files.delete(p);
                                    return fileSize;
                                } catch (IOException e) {
                                    log.warn("[MCP-FileManager] 删除文件失败: {}", p, e);
                                    return 0;
                                }
                            })
                            .sum();
                } else {
                    // 只删除空目录
                    Files.delete(path);
                }
            } else {
                size = Files.size(path);
                Files.delete(path);
            }
            
            Map<String, Object> result = Map.of(
                    "filePath", filePath,
                    "isDirectory", isDirectory,
                    "recursive", Boolean.TRUE.equals(recursive),
                    "deletedSize", size,
                    "deleteTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            log.info("[MCP-FileManager] 成功删除: {}, 类型: {}, 大小: {} 字节", 
                    filePath, isDirectory ? "目录" : "文件", size);
            return LQMCPToolManager.MCPToolResponse.success("删除成功", result);
            
        } catch (IOException e) {
            log.error("[MCP-FileManager] 删除失败: {}", filePath, e);
            return LQMCPToolManager.MCPToolResponse.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     */
    @MCPToolMethod(name = "getFileInfo", description = "获取指定文件或目录的详细信息")
    public LQMCPToolManager.MCPToolResponse getFileInfo(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return LQMCPToolManager.MCPToolResponse.error("文件路径不能为空");
            }
            
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return LQMCPToolManager.MCPToolResponse.error("文件或目录不存在: " + filePath);
            }
            
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("path", path.toString());
            fileInfo.put("absolutePath", path.toAbsolutePath().toString());
            fileInfo.put("name", path.getFileName().toString());
            fileInfo.put("parent", path.getParent() != null ? path.getParent().toString() : null);
            fileInfo.put("isDirectory", Files.isDirectory(path));
            fileInfo.put("isFile", Files.isRegularFile(path));
            fileInfo.put("isSymbolicLink", Files.isSymbolicLink(path));
            fileInfo.put("size", Files.isRegularFile(path) ? Files.size(path) : 0);
            fileInfo.put("lastModified", Files.getLastModifiedTime(path).toString());
            fileInfo.put("creationTime", Files.readAttributes(path, "creationTime").get("creationTime").toString());
            fileInfo.put("readable", Files.isReadable(path));
            fileInfo.put("writable", Files.isWritable(path));
            fileInfo.put("executable", Files.isExecutable(path));
            
            if (Files.isDirectory(path)) {
                try {
                    long fileCount = Files.list(path).count();
                    fileInfo.put("childCount", fileCount);
                } catch (IOException e) {
                    fileInfo.put("childCount", "无法获取");
                }
            }
            
            Map<String, Object> result = Map.of(
                    "fileInfo", fileInfo,
                    "queryTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            log.info("[MCP-FileManager] 成功获取文件信息: {}", filePath);
            return LQMCPToolManager.MCPToolResponse.success("文件信息获取成功", result);
            
        } catch (IOException e) {
            log.error("[MCP-FileManager] 获取文件信息失败: {}", filePath, e);
            return LQMCPToolManager.MCPToolResponse.error("获取文件信息失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件
     */
    @MCPToolMethod(name = "searchFiles", description = "在指定目录中搜索文件")
    public LQMCPToolManager.MCPToolResponse searchFiles(String dirPath, String pattern, Boolean recursive) {
        try {
            if (dirPath == null || dirPath.trim().isEmpty()) {
                return LQMCPToolManager.MCPToolResponse.error("目录路径不能为空");
            }
            
            if (pattern == null || pattern.trim().isEmpty()) {
                return LQMCPToolManager.MCPToolResponse.error("搜索模式不能为空");
            }
            
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                return LQMCPToolManager.MCPToolResponse.error("目录不存在: " + dirPath);
            }
            
            if (!Files.isDirectory(path)) {
                return LQMCPToolManager.MCPToolResponse.error("不是有效的目录: " + dirPath);
            }
            
            List<Map<String, Object>> matchedFiles = new ArrayList<>();
            
            if (Boolean.TRUE.equals(recursive)) {
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().contains(pattern))
                        .forEach(p -> {
                            try {
                                Map<String, Object> fileInfo = new HashMap<>();
                                fileInfo.put("name", p.getFileName().toString());
                                fileInfo.put("path", p.toString());
                                fileInfo.put("size", Files.size(p));
                                fileInfo.put("lastModified", Files.getLastModifiedTime(p).toString());
                                matchedFiles.add(fileInfo);
                            } catch (IOException e) {
                                log.warn("[MCP-FileManager] 获取搜索文件信息失败: {}", p, e);
                            }
                        });
            } else {
                Files.list(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().contains(pattern))
                        .forEach(p -> {
                            try {
                                Map<String, Object> fileInfo = new HashMap<>();
                                fileInfo.put("name", p.getFileName().toString());
                                fileInfo.put("path", p.toString());
                                fileInfo.put("size", Files.size(p));
                                fileInfo.put("lastModified", Files.getLastModifiedTime(p).toString());
                                matchedFiles.add(fileInfo);
                            } catch (IOException e) {
                                log.warn("[MCP-FileManager] 获取搜索文件信息失败: {}", p, e);
                            }
                        });
            }
            
            Map<String, Object> result = Map.of(
                    "directory", dirPath,
                    "pattern", pattern,
                    "recursive", Boolean.TRUE.equals(recursive),
                    "matchedFiles", matchedFiles,
                    "count", matchedFiles.size(),
                    "searchTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            log.info("[MCP-FileManager] 搜索完成: 目录={}, 模式={}, 找到={} 个文件", 
                    dirPath, pattern, matchedFiles.size());
            return LQMCPToolManager.MCPToolResponse.success("文件搜索完成", result);
            
        } catch (IOException e) {
            log.error("[MCP-FileManager] 文件搜索失败: 目录={}, 模式={}", dirPath, pattern, e);
            return LQMCPToolManager.MCPToolResponse.error("文件搜索失败: " + e.getMessage());
        }
    }
}
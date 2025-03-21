package cn.lingque.cloud.config;

import lombok.Data;

/**
 * 配置信息
 */
@Data
public class DataId {
    private String dataId = "";
    private String type = "yaml";
    private String groupId = "default";
}
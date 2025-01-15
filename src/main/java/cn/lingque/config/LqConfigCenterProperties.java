package cn.lingque.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LqConfigCenterProperties {
    /**
     * 文件ids
     */
    private List<String> dataIds = new ArrayList<>();
}

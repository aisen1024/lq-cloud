package com.lingque.cloud.node.bean;

import cn.hutool.json.JSONUtil;
import com.lingque.util.LQUtil;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LQNodeInfo {
    private String serverName;
    private String nodeIp;
    private Integer nodePort;

    @Override
    public boolean equals(Object o) {
       return JSONUtil.toJsonStr(this).equals(JSONUtil.toJsonStr(o));
    }

    public String nodeId(){
       return LQUtil.getMD5(JSONUtil.toJsonStr(this));
    }
}

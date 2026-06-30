package cn.lingque.cloud.node.bean;

import cn.hutool.json.JSONUtil;
import cn.lingque.util.LQUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.net.URI;

@Data
@Accessors(chain = true)
public class LQNodeInfo {
    private String serverName;
    private String nodeIp;
    private Integer nodePort;

    @Override
    public boolean equals(Object o) {
       return this.toString().equals(JSONUtil.toJsonStr(o));
    }

    public String nodeId(){
       return LQUtil.getMD5(JSONUtil.toJsonStr(this));
    }

    @Override
    public String toString(){
        return JSONUtil.toJsonStr(this);
    }

    public URI getUri(String protocol) {
        return URI.create(protocol + "://" + nodeIp + ":" + nodePort);
    }
}

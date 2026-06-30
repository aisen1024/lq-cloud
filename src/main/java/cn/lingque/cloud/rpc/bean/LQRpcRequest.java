package cn.lingque.cloud.rpc.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

/**
 * LQ RPC请求对象
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Accessors(chain = true)
public class LQRpcRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 追踪ID */
    private String traceId;
    
    /** 服务名称 */
    private String serviceName;
    
    /** 方法名称 */
    private String methodName;
    
    /** 方法参数 */
    private Object[] args;
    
    /** 参数类型 */
    private Class<?>[] argTypes;
    
    /** 请求时间戳 */
    private Long timestamp;
    
    /** 客户端IP */
    private String clientIp;
    
    /** 服务版本 */
    private String version;
    
    /** 服务分组 */
    private String group;
    
    /** 扩展属性 */
    private Map<String, Object> attachments;
    
    /** 超时时间 */
    private Integer timeout;
    
    /** 是否异步调用 */
    private Boolean async;
    
    @Override
    public String toString() {
        return "LQRpcRequest{" +
                "traceId='" + traceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", args=" + Arrays.toString(args) +
                ", timestamp=" + timestamp +
                ", clientIp='" + clientIp + '\'' +
                ", version='" + version + '\'' +
                ", group='" + group + '\'' +
                ", timeout=" + timeout +
                ", async=" + async +
                '}';
    }
}
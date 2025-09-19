package cn.lingque.cloud.rpc.example;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息实体类
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Accessors(chain = true)
public class UserInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 用户ID */
    private Long userId;
    
    /** 用户名 */
    private String username;
    
    /** 密码 */
    private String password;
    
    /** 邮箱 */
    private String email;
    
    /** 手机号 */
    private String phone;
    
    /** 真实姓名 */
    private String realName;
    
    /** 年龄 */
    private Integer age;
    
    /** 性别 (1:男, 2:女) */
    private Integer gender;
    
    /** 头像URL */
    private String avatar;
    
    /** 状态 (1:正常, 0:禁用) */
    private Integer status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
    
    /** 登录IP */
    private String loginIp;
    
    /** 备注 */
    private String remark;
}
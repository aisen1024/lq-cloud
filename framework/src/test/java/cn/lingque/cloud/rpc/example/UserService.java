package cn.lingque.cloud.rpc.example;

import cn.lingque.cloud.rpc.annotation.LQService;
import cn.lingque.cloud.rpc.annotation.LQServiceMethod;

import java.util.List;

/**
 * 用户服务接口示例
 * 展示如何定义可远程调用的服务接口
 * 
 * @author aisen
 * @date 2024-12-19
 */
@LQService(value = "user-service", version = "1.0.0")
public interface UserService {
    
    /**
     * 根据ID获取用户信息
     */
    @LQServiceMethod(timeout = 3000)
    UserInfo getUserById(Long userId);
    
    /**
     * 创建用户
     */
    @LQServiceMethod(timeout = 5000, retryCount = 2)
    Long createUser(UserInfo userInfo);
    
    /**
     * 更新用户信息
     */
    @LQServiceMethod
    boolean updateUser(UserInfo userInfo);
    
    /**
     * 删除用户
     */
    @LQServiceMethod
    boolean deleteUser(Long userId);
    
    /**
     * 获取用户列表
     */
    @LQServiceMethod(timeout = 10000, cache = true, cacheExpire = 600)
    List<UserInfo> getUserList(int page, int size);
    
    /**
     * 用户登录
     */
    @LQServiceMethod(timeout = 3000, retryCount = 1)
    String login(String username, String password);
    
    /**
     * 批量获取用户信息
     */
    @LQServiceMethod(timeout = 8000)
    List<UserInfo> batchGetUsers(List<Long> userIds);
}
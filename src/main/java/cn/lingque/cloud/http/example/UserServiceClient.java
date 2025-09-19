package cn.lingque.cloud.http.example;

import cn.lingque.cloud.http.annotation.*;
import cn.lingque.runner.annon.LqService;

/**
 * 用户服务客户端示例
 * 演示如何使用新的HTTP客户端注解替代Forest
 */
@LqService(serviceName = "user-service", connectTimeout = 5000, readTimeout = 30000, retryCount = 1)
public interface UserServiceClient {

    /**
     * 获取用户信息
     */
    @Get("/api/user/{id}")
    String getUserById(@PathVariable("id") Long userId);

    /**
     * 创建用户
     */
    @Post(value = "/api/user", contentType = "application/json")
    String createUser(@RequestBody String userJson);

    /**
     * 更新用户
     */
    @Put(value = "/api/user/{id}", contentType = "application/json")
    String updateUser(@PathVariable("id") Long userId, @RequestBody String userJson);

    /**
     * 删除用户
     */
    @Delete("/api/user/{id}")
    String deleteUser(@PathVariable("id") Long userId);

    /**
     * 查询用户列表
     */
    @Get("/api/users")
    String getUserList(@RequestParam("page") int page, 
                      @RequestParam("size") int size,
                      @RequestParam(value = "keyword", required = false) String keyword);
}
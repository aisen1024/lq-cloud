package cn.lingque.cloud.http.example;

import cn.lingque.cloud.http.annotation.Delete;
import cn.lingque.cloud.http.annotation.Get;
import cn.lingque.cloud.http.annotation.PathVariable;
import cn.lingque.cloud.http.annotation.Post;
import cn.lingque.cloud.http.annotation.Put;
import cn.lingque.cloud.http.annotation.RequestBody;
import cn.lingque.cloud.http.annotation.RequestParam;
import cn.lingque.runner.annon.LqService;

/**
 * 用户API客户端示例
 * 展示如何使用HTTP客户端框架进行远程调用
 */
@LqService(serviceName = "user-api-service")
public interface UserApiClient {

    /**
     * 获取用户信息（需要认证）
     * 返回ApiResult<User>格式的响应
     */
    @Get("/api/user/{id}")
    ApiResult<User> getUserById(@PathVariable("id") Long userId);

    /**
     * 获取用户列表（需要认证）
     * 支持分页查询
     */
    @Get("/api/users")
    ApiResult<PageResult<User>> getUserList(
        @RequestParam("page") Integer page,
        @RequestParam("size") Integer size,
        @RequestParam(value = "keyword", required = false) String keyword
    );

    /**
     * 创建用户（需要认证）
     */
    @Post("/api/user")
    ApiResult<User> createUser(@RequestBody CreateUserRequest request);

    /**
     * 更新用户信息（需要认证）
     */
    @Put("/api/user/{id}")
    ApiResult<User> updateUser(
        @PathVariable("id") Long userId,
        @RequestBody UpdateUserRequest request
    );

    /**
     * 删除用户（需要认证）
     */
    @Delete("/api/user/{id}")
    ApiResult<Void> deleteUser(@PathVariable("id") Long userId);

    /**
     * 用户登录（不需要认证）
     */
    @Post("/api/auth/login")
    ApiResult<LoginResponse> login(@RequestBody LoginRequest request);

    /**
     * 用户登出（需要认证）
     */
    @Post("/api/auth/logout")
    ApiResult<Void> logout();

    /**
     * 获取当前用户信息（需要认证）
     */
    @Get("/api/auth/me")
    ApiResult<User> getCurrentUser();

    // ========== 数据模型 ==========

    /**
     * API响应结果
     */
    class ApiResult<T> {
        private Integer code;
        private String message;
        private T data;
        private Boolean success;

        // getters and setters
        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
    }

    /**
     * 分页结果
     */
    class PageResult<T> {
        private java.util.List<T> records;
        private Long total;
        private Integer page;
        private Integer size;

        // getters and setters
        public java.util.List<T> getRecords() { return records; }
        public void setRecords(java.util.List<T> records) { this.records = records; }
        
        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
        
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    /**
     * 用户实体
     */
    class User {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;

        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public java.time.LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
        
        public java.time.LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    }

    /**
     * 创建用户请求
     */
    class CreateUserRequest {
        private String username;
        private String password;
        private String email;
        private String nickname;

        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    /**
     * 更新用户请求
     */
    class UpdateUserRequest {
        private String email;
        private String nickname;

        // getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    /**
     * 登录请求
     */
    class LoginRequest {
        private String username;
        private String password;

        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 登录响应
     */
    class LoginResponse {
        private String token;
        private String tokenType;
        private Long expiresIn;
        private User userInfo;

        // getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        
        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
        
        public User getUserInfo() { return userInfo; }
        public void setUserInfo(User userInfo) { this.userInfo = userInfo; }
    }
}
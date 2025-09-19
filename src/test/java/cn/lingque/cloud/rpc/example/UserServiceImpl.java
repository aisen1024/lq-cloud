package cn.lingque.cloud.rpc.example;

import cn.lingque.cloud.rpc.annotation.LQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 展示如何实现可远程调用的服务
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Service
@LQService("user-service")
public class UserServiceImpl implements UserService {
    
    /** 模拟数据库存储 */
    private final Map<Long, UserInfo> userDatabase = new ConcurrentHashMap<>();
    
    /** ID生成器 */
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserServiceImpl() {
        // 初始化一些测试数据
        initTestData();
    }
    
    @Override
    public UserInfo getUserById(Long userId) {
        log.info("获取用户信息: userId={}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        UserInfo userInfo = userDatabase.get(userId);
        if (userInfo == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        
        return userInfo;
    }
    
    @Override
    public Long createUser(UserInfo userInfo) {
        log.info("创建用户: {}", userInfo.getUsername());
        
        if (userInfo == null || userInfo.getUsername() == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        
        // 检查用户名是否已存在
        boolean exists = userDatabase.values().stream()
                .anyMatch(u -> u.getUsername().equals(userInfo.getUsername()));
        if (exists) {
            throw new RuntimeException("用户名已存在: " + userInfo.getUsername());
        }
        
        // 生成用户ID
        Long userId = idGenerator.getAndIncrement();
        userInfo.setUserId(userId);
        userInfo.setCreateTime(LocalDateTime.now());
        userInfo.setUpdateTime(LocalDateTime.now());
        userInfo.setStatus(1); // 默认正常状态
        
        // 保存用户
        userDatabase.put(userId, userInfo);
        
        log.info("用户创建成功: userId={}, username={}", userId, userInfo.getUsername());
        return userId;
    }
    
    @Override
    public boolean updateUser(UserInfo userInfo) {
        log.info("更新用户信息: userId={}", userInfo.getUserId());
        
        if (userInfo == null || userInfo.getUserId() == null) {
            throw new IllegalArgumentException("用户信息或用户ID不能为空");
        }
        
        UserInfo existingUser = userDatabase.get(userInfo.getUserId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在: " + userInfo.getUserId());
        }
        
        // 更新用户信息
        userInfo.setUpdateTime(LocalDateTime.now());
        userDatabase.put(userInfo.getUserId(), userInfo);
        
        log.info("用户信息更新成功: userId={}", userInfo.getUserId());
        return true;
    }
    
    @Override
    public boolean deleteUser(Long userId) {
        log.info("删除用户: userId={}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        UserInfo removedUser = userDatabase.remove(userId);
        if (removedUser == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        
        log.info("用户删除成功: userId={}, username={}", userId, removedUser.getUsername());
        return true;
    }
    
    @Override
    public List<UserInfo> getUserList(int page, int size) {
        log.info("获取用户列表: page={}, size={}", page, size);
        
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("页码和页大小必须大于0");
        }
        
        List<UserInfo> allUsers = new ArrayList<>(userDatabase.values());
        
        // 简单分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, allUsers.size());
        
        if (start >= allUsers.size()) {
            return new ArrayList<>();
        }
        
        List<UserInfo> result = allUsers.subList(start, end);
        log.info("返回用户列表: count={}", result.size());
        return result;
    }
    
    @Override
    public String login(String username, String password) {
        log.info("用户登录: username={}", username);
        
        if (username == null || password == null) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        
        // 查找用户
        UserInfo user = userDatabase.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
        
        if (user == null) {
            throw new RuntimeException("用户不存在: " + username);
        }
        
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginIp("127.0.0.1"); // 简化处理
        
        // 生成简单的token（实际项目中应该使用JWT等）
        String token = "token_" + user.getUserId() + "_" + System.currentTimeMillis();
        
        log.info("用户登录成功: userId={}, username={}", user.getUserId(), username);
        return token;
    }
    
    @Override
    public List<UserInfo> batchGetUsers(List<Long> userIds) {
        log.info("批量获取用户信息: userIds={}", userIds);
        
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<UserInfo> result = userIds.stream()
                .map(userDatabase::get)
                .filter(user -> user != null)
                .collect(Collectors.toList());
        
        log.info("批量获取用户信息完成: 请求{}个，返回{}个", userIds.size(), result.size());
        return result;
    }
    
    /**
     * 初始化测试数据
     */
    private void initTestData() {
        // 创建一些测试用户
        for (int i = 1; i <= 5; i++) {
            UserInfo user = new UserInfo()
                    .setUserId((long) i)
                    .setUsername("user" + i)
                    .setPassword("password" + i)
                    .setEmail("user" + i + "@example.com")
                    .setPhone("1380000000" + i)
                    .setRealName("用户" + i)
                    .setAge(20 + i)
                    .setGender(i % 2 + 1)
                    .setStatus(1)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now());
            
            userDatabase.put((long) i, user);
        }
        
        // 更新ID生成器
        idGenerator.set(6);
        
        log.info("初始化测试数据完成，共{}个用户", userDatabase.size());
    }
}
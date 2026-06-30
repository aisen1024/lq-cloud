package com.paypay.lqadmin.controller;

import com.paypay.lqadmin.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Map<String, Object> result = new HashMap<>();
        
        if (authService.authenticate(username, password)) {
            String token = authService.generateToken(username);
            result.put("success", true);
            result.put("token", token);
            result.put("username", username);
            result.put("message", "登录成功");
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> result = new HashMap<>();
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            authService.invalidateToken(token);
        }
        
        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    @GetMapping("/verify")
    public Map<String, Object> verify(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> result = new HashMap<>();
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String username = authService.validateToken(token);
            
            if (username != null) {
                result.put("success", true);
                result.put("username", username);
                return result;
            }
        }
        
        result.put("success", false);
        result.put("message", "Token 无效或已过期");
        return result;
    }
}

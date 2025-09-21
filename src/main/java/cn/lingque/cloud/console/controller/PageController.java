package cn.lingque.cloud.console.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由控制器
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Controller
public class PageController {
    
    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        return "forward:/login.html";
    }
    
    /**
     * 管理控制台首页
     */
    @GetMapping({"/", "/console", "/admin"})
    public String consolePage() {
        return "forward:/index.html";
    }
}
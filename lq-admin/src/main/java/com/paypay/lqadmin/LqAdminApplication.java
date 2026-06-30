package com.paypay.lqadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LingQue Admin 管理和治理服务
 * 提供配置中心、服务中心、消息总线、消息队列的管理界面和 API
 */
@SpringBootApplication(scanBasePackages = {"com.paypay.lqadmin"})
public class LqAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LqAdminApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("LingQue Admin 启动成功！");
        System.out.println("管理界面: http://localhost:8080");
        System.out.println("API 文档: http://localhost:8080/api");
        System.out.println("========================================\n");
    }

}

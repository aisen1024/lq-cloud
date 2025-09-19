package cn.lingque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * LingQue Cloud 主应用程序
 * 
 * @author LingQue Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = "cn.lingque.cloud")
public class LQCloudApplication {
    
    public static void main(String[] args) {
        System.out.println("\n" +
            "=====================================\n" +
            "  🚀 LingQue Cloud 正在启动...\n" +
            "=====================================\n");
        
        SpringApplication app = new SpringApplication(LQCloudApplication.class);
        app.run(args);
        
        System.out.println("\n" +
            "=====================================\n" +
            "  ✅ LingQue Cloud 启动成功！\n" +
            "  📊 管理控制台: http://localhost:8080\n" +
            "  👤 默认账号: admin / admin123\n" +
            "=====================================\n");
    }
}
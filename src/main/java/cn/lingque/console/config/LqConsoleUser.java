package cn.lingque.console.config;

import lombok.Data;

@Data
public class LqConsoleUser {
    /**
     * 登陆名
     */
    private String username = "lingque";
    /***
     * 密码
     */
    private String password= "123456";

    /**
     * 权限 all全部 config 配置中心 server服务中心 doc文档
     */
    private String powers = "all";

    /**
     * 权限 指定服务列表，只针对 server服务中心 doc文档，空代表全部服务都可访问，
     */
    private String serverList = "";

}

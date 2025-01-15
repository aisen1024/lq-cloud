package cn.lingque.console.config;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LqConsoleConfig {

    /**
     * 用户列表
     */
    private List<LqConsoleUser> users = new ArrayList<>();

    /**
     * 密码错误次数，超过就封IP一天
     */
    private int tryTimes = 3;
    /**
     * 是否开启控制台
     */
    private boolean enable = true;
    /**
     * 白名单IP,只允许白名单访问
     */
    private List<String> ips = new ArrayList<>();
}

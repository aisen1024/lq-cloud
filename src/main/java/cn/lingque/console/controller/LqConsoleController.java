package cn.lingque.console.controller;

import cn.lingque.base.LQKey;
import cn.lingque.console.config.LqConsoleUser;
import cn.lingque.console.service.LqConsoleService;
import cn.lingque.console.vo.Login;
import cn.lingque.console.vo.Result;
import cn.lingque.util.IpUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RequestMapping("/lq/console")
@RestController
@ConditionalOnProperty(value = "ling-que.console.enable",havingValue = "true")
public class LqConsoleController {

    @PostConstruct
    public void init(){
        log.info("LqConsoleController init...");
    }

    @Resource
    private LqConsoleService consoleService;


    private final static LQKey LQ_LOGIN_TOKEN = LQKey.key("lq:login:token:console",1D,LQKey.ONE_DAY);
    private final static LQKey LQ_LOGIN_TIMES = LQKey.key("lq:login:times:console",1D,LQKey.ONE_DAY);

    @PostMapping("login")
    public Result login(@RequestBody Login login, HttpServletRequest request){
        if (LQ_LOGIN_TIMES.rd(IpUtil.getRealIpAddr(request)).ofValue().getValue(Integer.class,0) >= consoleService.getMaxTryTime()){
            return Result.error("您登陆次数过多，已被封禁，请明天重试！");
        }
        LqConsoleUser user = consoleService.findUserByUsername(login.getUsername(),login.getPassword());
        if (user != null){
            String token = UUID.randomUUID().toString().replace("-","");
            LQ_LOGIN_TOKEN.rd(token).ofValue().set(user);
            LQ_LOGIN_TIMES.rd(IpUtil.getRealIpAddr(request)).delete();
            return Result.ok(token);
        }
        LQ_LOGIN_TIMES.rd(IpUtil.getRealIpAddr(request)).ofValue().incr();
        return Result.error("用户名或者密码不对!");
    }



}

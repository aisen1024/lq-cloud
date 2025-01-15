package cn.lingque.console.service;

import cn.lingque.config.LQProperties;
import cn.lingque.console.config.LqConsoleUser;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class LqConsoleService {

    @Resource
    private LQProperties lqProperties;

    public LqConsoleUser findUserByUsername(String username,String password) {
      return lqProperties.getConsole().getUsers().stream().filter(user -> user.getUsername().equals(username) && user.getPassword().equalsIgnoreCase(password)).findFirst().get();
    }

    public int getMaxTryTime(){
        return lqProperties.getConsole().getTryTimes();
    }

}

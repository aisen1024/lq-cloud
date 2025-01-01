package cn.lingque.scene;

import cn.lingque.redis.LingQueRedis;
import cn.lingque.scene.exten.ClockScene;
import cn.lingque.scene.exten.SIdScene;
import lombok.AllArgsConstructor;

/**
 * @author aisen
 * @date 2024/9/27
 * @desc 简单说一下
 **/
@AllArgsConstructor
public class LQScene {

    private LingQueRedis lingQueRedis;

    public ClockScene ofClock(){
        return new ClockScene(lingQueRedis);
    }

    public SIdScene ofSid(){
        return new SIdScene(lingQueRedis);
    }


}

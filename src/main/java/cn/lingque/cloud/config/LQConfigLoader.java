package cn.lingque.cloud.config;


import cn.lingque.base.LQKey;
import cn.lingque.config.LQProperties;
import cn.lingque.runner.annon.LqEvenBus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置加载类
 */
@Component
public class LQConfigLoader {

    private volatile static List<DataId> dataIds = new ArrayList<>();
    private final static String EVENT_KEY = "lq:config:loader";
    private final static LQKey CONFIG_KEY = LQKey.key("lq:config:subscribe",1D,LQKey.ONE_HOUR);

    @LqEvenBus(even = "lq:config:loader")
    public void subscribe(String yaml){

    }

    public static void start(LQProperties properties){

    }

    public static void register(DataId dataId){
        dataIds.add(dataId);
    }

    public static void pushData(String yaml){

    }


}

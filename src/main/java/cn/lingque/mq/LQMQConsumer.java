package cn.lingque.mq;

import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.mq.exten.itf.IMQConsumer;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aisen
 * @date 2024/10/9
 * @desc 消费者
 **/
public class LQMQConsumer {

    private volatile static Map<String, IMQConsumer> registerConsumerMap = new HashMap<>();
    private volatile static Map<String, List<ILQMessage>> registerHandleMap = new HashMap<>();

    //状态，true MQ正常消费，false 暂停消费
    private volatile static AtomicBoolean STATE = new AtomicBoolean(true);

    //是否运行 true 已经开启了registerMap遍历执行
    private volatile static AtomicBoolean IS_RUN = new AtomicBoolean(false);

    /**
     * 注册MQ处理器
     * @param key
     * @param handle
     */
    public static void register(String key, IMQConsumer consumer, ILQMessage handle){
        //限定，LingQueMQConsumer 一个key只能允许一个
        if (handle instanceof LQMQConsumer){
            if (registerHandleMap.get(key) != null){
                throw new RuntimeException("TimingWheelQueue is already registered! ,please check it ->"+key);
            }
        }
        LQUtil.getMapList(registerHandleMap,key).add(handle);
        registerConsumerMap.put(key, consumer);
    }

    /**
     * 启动MQ消息
     */
    public static void start() {
        STATE.compareAndSet(false, true);
        if (IS_RUN.compareAndSet(false, true)) {
            LQUtil.execLoadJob("消息消费任务",()->{
                    if (registerConsumerMap.size() > 0 && STATE.get()) {
                        for (String key : registerConsumerMap.keySet()) {
                            IMQConsumer consumer = registerConsumerMap.get(key);
                            if (consumer == null) {
                                continue;
                            }
                            TryCatch.trying(() -> {
                                LQThreadUtil.execMaster(() -> TryCatch.trying(() -> consumer.consumer(registerHandleMap.get(key)), "订阅{}主题，出现异常", key));
                            });
                        }
                    }
            },1000,50,TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 停止MQ消息消费
     */
    public static void stop(){
        STATE.compareAndSet(true,false);
    }


}

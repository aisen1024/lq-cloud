package com.lingque.thread;

import com.lingque.exceptions.LQException;
import lombok.Data;


/**
 * @author aisen
 * @date 2024/9/24
 * @desc 线程池
 **/
@Data
public class LQThreadUtil {

    private static LQThread LQThread;


    public static void init(LQThread LQThread){
        LQThreadUtil.LQThread = LQThread;
    }

    /**
     * 主线程池
     * @param runnable
     */
    public static void execMaster(Runnable runnable){
        if (null == LQThread){
            throw new LQException("请初始化LingQueThread线程池！");
        }
        LQThread.execMaster(runnable);
    }

    /**
     * 辅线程池
     * @param runnable
     */
    public static void execSlave(Runnable runnable){
        if (null == LQThread){
            throw new LQException("请初始化LingQueThread线程池！");
        }
        LQThread.execSlave(runnable);
    }





}
